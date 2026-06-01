package com.cmulagent.core.agent;

import com.cmulagent.context.ContextManager;
import com.cmulagent.core.tool.ToolRegistry;
import com.cmulagent.event.WebSocketHandler;
import com.cmulagent.llm.LLMClient;
import com.cmulagent.llm.LLMClientFactory;
import com.cmulagent.persistence.AgentExecutionRepository;
import com.cmulagent.persistence.AgentSpecEntity;
import com.cmulagent.persistence.AgentSpecRepository;
import com.cmulagent.persistence.MessageRecordRepository;
import com.cmulagent.persistence.ToolInvocationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final LLMClientFactory llmClientFactory;
    private final AgentSpecRepository agentSpecRepository;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;
    private final WebSocketHandler webSocketHandler;
    private final AgentExecutionRepository executionRepository;
    private final MessageRecordRepository messageRepository;
    private final ToolInvocationRepository toolInvocationRepository;

    public AgentOrchestrator(LLMClientFactory llmClientFactory,
                             AgentSpecRepository agentSpecRepository,
                             ToolRegistry toolRegistry,
                             ContextManager contextManager,
                             WebSocketHandler webSocketHandler,
                             AgentExecutionRepository executionRepository,
                             MessageRecordRepository messageRepository,
                             ToolInvocationRepository toolInvocationRepository) {
        this.llmClientFactory = llmClientFactory;
        this.agentSpecRepository = agentSpecRepository;
        this.toolRegistry = toolRegistry;
        this.contextManager = contextManager;
        this.webSocketHandler = webSocketHandler;
        this.executionRepository = executionRepository;
        this.messageRepository = messageRepository;
        this.toolInvocationRepository = toolInvocationRepository;
    }

    public CompletableFuture<String> executeWithAgent(String agentSpecIdOrName, String subtaskId, String input) {
        AgentSpecEntity specEntity = agentSpecRepository.findById(agentSpecIdOrName)
                .or(() -> agentSpecRepository.findByName(agentSpecIdOrName))
                .orElseThrow(() -> new IllegalArgumentException("Agent spec not found: " + agentSpecIdOrName));

        AgentSpec spec = toAgentSpec(specEntity);
        LLMClient llmClient = createLLMClient(specEntity);
        AgentExecutor executor = new AgentExecutor(spec, llmClient, toolRegistry, contextManager,
                webSocketHandler, executionRepository, messageRepository, toolInvocationRepository);

        log.info("Dispatching subtask {} to agent {} ({})", subtaskId, spec.getName(), spec.getId());
        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = executor.execute(input);
                log.info("Subtask {} completed by agent {}", subtaskId, spec.getName());
                return result;
            } catch (Exception e) {
                log.error("Subtask {} execution failed by agent {}", subtaskId, spec.getName(), e);
                throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
            }
        });
    }

    public LLMClient createLLMClient(AgentSpecEntity spec) {
        String baseUrl = spec.getBaseUrl();
        String apiKey = spec.getApiKey();
        String model = spec.getModel();

        if (apiKey != null && apiKey.startsWith("sk-ant")) {
            return llmClientFactory.createAnthropic(apiKey, model);
        }
        if (baseUrl != null && (baseUrl.contains("ollama") || baseUrl.contains("11434"))) {
            return llmClientFactory.createOllama(baseUrl, model);
        }
        if (baseUrl != null && apiKey != null) {
            return llmClientFactory.createOpenAiCompat(baseUrl, apiKey, model);
        }
        throw new IllegalArgumentException("Cannot determine LLM adapter for agent spec: " + spec.getId());
    }

    private AgentSpec toAgentSpec(AgentSpecEntity entity) {
        List<String> tools = List.of();
        if (entity.getTools() != null && !entity.getTools().isBlank()) {
            try {
                tools = mapper.readValue(entity.getTools(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse tools JSON for agent spec {}: {}", entity.getId(), e.getMessage());
            }
        }
        return AgentSpec.builder()
                .id(entity.getId())
                .name(entity.getName())
                .role(entity.getRole())
                .baseUrl(entity.getBaseUrl())
                .model(entity.getModel())
                .apiKey(entity.getApiKey())
                .tools(tools)
                .maxSteps(entity.getMaxSteps())
                .outputFormat(entity.getOutputFormat())
                .enabled(entity.getEnabled())
                .build();
    }
}