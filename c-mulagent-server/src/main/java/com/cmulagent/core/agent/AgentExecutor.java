package com.cmulagent.core.agent;

import com.cmulagent.context.ContextManager;
import com.cmulagent.core.tool.ToolExecutionException;
import com.cmulagent.core.tool.ToolRegistry;
import com.cmulagent.event.AgentEvent;
import com.cmulagent.event.AgentEventType;
import com.cmulagent.event.WebSocketHandler;
import com.cmulagent.llm.LLMClient;
import com.cmulagent.llm.LLMClient.Message;
import com.cmulagent.persistence.AgentExecutionEntity;
import com.cmulagent.persistence.AgentExecutionRepository;
import com.cmulagent.persistence.MessageRecordEntity;
import com.cmulagent.persistence.MessageRecordRepository;
import com.cmulagent.persistence.ToolInvocationEntity;
import com.cmulagent.persistence.ToolInvocationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"params\"\\s*:\\s*(\\{[^}]*\\})\\s*\\}");

    private final AgentSpec agentSpec;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;
    private final WebSocketHandler webSocketHandler;
    private final AgentExecutionRepository executionRepository;
    private final MessageRecordRepository messageRepository;
    private final ToolInvocationRepository toolInvocationRepository;

    private volatile AgentState state = AgentState.IDLE;
    private final String executionId;

    public AgentExecutor(
            AgentSpec agentSpec,
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            ContextManager contextManager,
            WebSocketHandler webSocketHandler,
            AgentExecutionRepository executionRepository,
            MessageRecordRepository messageRepository,
            ToolInvocationRepository toolInvocationRepository) {
        this.agentSpec = agentSpec;
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.contextManager = contextManager;
        this.webSocketHandler = webSocketHandler;
        this.executionRepository = executionRepository;
        this.messageRepository = messageRepository;
        this.toolInvocationRepository = toolInvocationRepository;
        this.executionId = UUID.randomUUID().toString();
    }

    public String getExecutionId() {
        return executionId;
    }

    public AgentState getState() {
        return state;
    }

    public void cancel() {
        log.info("Agent execution cancelled: executionId={}, agentSpecId={}", executionId, agentSpec.getId());
        state = AgentState.CANCELLED;
        updateExecutionStatus("CANCELLED", null);
        emitStateChangedEvent();
    }

    public String execute(String input) {
        long startTime = System.currentTimeMillis();
        String now = LocalDateTime.now().toString();

        AgentExecutionEntity execution = AgentExecutionEntity.builder()
                .id(executionId)
                .agentSpecId(agentSpec.getId())
                .status("RUNNING")
                .startTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        executionRepository.save(execution);

        state = AgentState.RUNNING;
        log.info("Agent execution started: executionId={}, agentSpecId={}, agentName={}",
                executionId, agentSpec.getId(), agentSpec.getName());
        emitEvent(AgentEventType.AGENT_STATE_CHANGED, Map.of(
                "state", "RUNNING",
                "agentId", agentSpec.getId(),
                "agentName", agentSpec.getName()
        ));

        try {
            String systemPrompt = buildSystemPrompt();
            List<Message> messages = new ArrayList<>();
            messages.add(new Message("user", input));

            String finalOutput = null;
            long totalEstimatedTokens = 0;

            for (int step = 0; step < getMaxSteps() && state == AgentState.RUNNING; step++) {
                log.debug("Agent step {}/{}: executionId={}", step + 1, getMaxSteps(), executionId);

                String response;
                try {
                    response = llmClient.chat(systemPrompt, new ArrayList<>(messages)).get();
                } catch (Exception e) {
                    log.error("LLM call failed at step {}: executionId={}", step + 1, executionId, e);
                    throw new RuntimeException("LLM call failed at step " + (step + 1) + ": " + e.getMessage(), e);
                }

                totalEstimatedTokens += estimateTokens(response);

                Message assistantMessage = new Message("assistant", response);
                messages.add(assistantMessage);

                saveMessage("assistant", response, null);

                StackTraceElement caller = Thread.currentThread().getStackTrace()[1];
                emitEvent(AgentEventType.TASK_PROGRESS, Map.of(
                        "step", step + 1,
                        "maxSteps", getMaxSteps(),
                        "message", response.length() > 200 ? response.substring(0, 200) + "..." : response
                ));

                Map<String, Object> toolCall = extractToolCall(response);
                if (toolCall != null) {
                    String toolName = (String) toolCall.get("tool");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> toolParams = (Map<String, Object>) toolCall.get("params");

                    log.info("Agent invoking tool: executionId={}, tool={}", executionId, toolName);
                    emitEvent(AgentEventType.AGENT_TOOL_INVOKED, Map.of(
                            "toolName", toolName,
                            "params", toolParams != null ? toolParams : Map.of()
                    ));

                    String toolInvocationId = UUID.randomUUID().toString();
                    String toolInvocationCreatedAt = LocalDateTime.now().toString();
                    saveToolInvocation(toolInvocationId, toolName, toolParams, "PENDING", null);

                    long toolStart = System.currentTimeMillis();
                    String toolResult;
                    String toolStatus;
                    String toolErrorMsg = null;

                    try {
                        toolResult = toolRegistry.invoke(toolName, toolParams);
                        toolStatus = "SUCCESS";
                    } catch (ToolExecutionException e) {
                        log.error("Tool execution failed: executionId={}, tool={}", executionId, toolName, e);
                        toolResult = "Error: " + e.getMessage();
                        toolStatus = "FAILED";
                        toolErrorMsg = e.getMessage();
                    } catch (Exception e) {
                        log.error("Unexpected tool error: executionId={}, tool={}", executionId, toolName, e);
                        toolResult = "Error: " + e.getMessage();
                        toolStatus = "FAILED";
                        toolErrorMsg = e.getMessage();
                    }

                    long toolDuration = System.currentTimeMillis() - toolStart;
                    updateToolInvocation(toolInvocationId, toolStatus, toolResult, toolDuration, toolErrorMsg);

                    emitEvent(AgentEventType.AGENT_TOOL_RESULT, Map.of(
                            "toolName", toolName,
                            "status", toolStatus,
                            "result", toolResult
                    ));

                    Message toolMessage = new Message("user",
                            "Tool result for " + toolName + ": " + toolResult);
                    messages.add(toolMessage);
                } else {
                    finalOutput = response;
                    break;
                }
            }

            if (state == AgentState.CANCELLED) {
                long duration = System.currentTimeMillis() - startTime;
                updateExecutionCompleted("CANCELLED", duration, null);
                return null;
            }

            if (finalOutput == null) {
                finalOutput = messages.get(messages.size() - 1).content();
                log.warn("Agent reached max steps without final output: executionId={}", executionId);
            }

            long duration = System.currentTimeMillis() - startTime;
            updateExecutionCompleted("COMPLETED", duration, null);
            state = AgentState.COMPLETED;
            emitEvent(AgentEventType.AGENT_STATE_CHANGED, Map.of(
                    "state", "COMPLETED",
                    "agentId", agentSpec.getId(),
                    "outputLength", finalOutput.length(),
                    "totalTokens", totalEstimatedTokens,
                    "steps", messages.size() / 2,
                    "durationMs", duration
            ));

            log.info("Agent execution completed: executionId={}, durationMs={}, steps={}",
                    executionId, duration, messages.size() / 2);

            return finalOutput;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            log.error("Agent execution failed: executionId={}, durationMs={}", executionId, duration, e);

            updateExecutionCompleted("FAILED", duration, errorMsg);
            state = AgentState.FAILED;
            emitEvent(AgentEventType.AGENT_STATE_CHANGED, Map.of(
                    "state", "FAILED",
                    "agentId", agentSpec.getId(),
                    "error", errorMsg
            ));

            throw new RuntimeException("Agent execution failed: " + errorMsg, e);
        }
    }

    private int getMaxSteps() {
        return Math.max(1, agentSpec.getMaxSteps() != null ? agentSpec.getMaxSteps() : 10);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI agent with the following role:\n");
        sb.append(agentSpec.getRole()).append("\n\n");

        List<com.cmulagent.core.tool.ToolSpec> availableTools = getAvailableTools();
        if (!availableTools.isEmpty()) {
            sb.append("You have access to the following tools:\n");
            for (com.cmulagent.core.tool.ToolSpec tool : availableTools) {
                sb.append("- ").append(tool.getName());
                if (tool.getDescription() != null && !tool.getDescription().isBlank()) {
                    sb.append(": ").append(tool.getDescription());
                }
                if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                    sb.append("\n  Parameters: ").append(toJson(tool.getInputSchema()));
                }
                sb.append("\n");
            }
            sb.append("\nTo use a tool, respond with a JSON object in this exact format:\n");
            sb.append("{\"tool\": \"tool_name\", \"params\": {\"param1\": \"value1\"}}\n");
            sb.append("Only one tool call per response. Do not include any other text when calling a tool.\n\n");
        }

        if (agentSpec.getOutputFormat() != null && !agentSpec.getOutputFormat().isBlank()) {
            sb.append("Output format requirement: ").append(agentSpec.getOutputFormat()).append("\n\n");
        }

        sb.append("Context data from shared memory:\n");
        sb.append(toJson(contextManager.getMap()));

        return sb.toString();
    }

    private List<com.cmulagent.core.tool.ToolSpec> getAvailableTools() {
        List<com.cmulagent.core.tool.ToolSpec> allSpecs = toolRegistry.getSpecs();
        if (agentSpec.getTools() == null || agentSpec.getTools().isEmpty()) {
            return allSpecs;
        }
        return allSpecs.stream()
                .filter(spec -> agentSpec.getTools().contains(spec.getName()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractToolCall(String response) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        if (matcher.find()) {
            try {
                String toolName = matcher.group(1);
                String paramsStr = matcher.group(2);
                Map<String, Object> params = objectMapper.readValue(paramsStr, Map.class);
                return Map.of("tool", toolName, "params", params);
            } catch (Exception e) {
                log.debug("Failed to parse tool call from response: executionId={}", executionId, e);
                return null;
            }
        }
        return null;
    }

    private void saveMessage(String role, String content, Integer tokenCount) {
        try {
            MessageRecordEntity entity = MessageRecordEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .agentExecutionId(executionId)
                    .role(role)
                    .content(content)
                    .model(agentSpec.getModel())
                    .tokenCount(tokenCount)
                    .createdAt(LocalDateTime.now().toString())
                    .build();
            messageRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to save message: executionId={}", executionId, e);
        }
    }

    private void saveToolInvocation(String id, String toolName, Map<String, Object> params, String status, String errorMessage) {
        try {
            ToolInvocationEntity entity = ToolInvocationEntity.builder()
                    .id(id)
                    .agentExecutionId(executionId)
                    .toolName(toolName)
                    .inputParams(toJson(params))
                    .status(status)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now().toString())
                    .build();
            toolInvocationRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to save tool invocation: executionId={}, tool={}", executionId, toolName, e);
        }
    }

    private void updateToolInvocation(String id, String status, String outputResult, long durationMs, String errorMessage) {
        try {
            toolInvocationRepository.updateResult(id, status, outputResult, durationMs, errorMessage);
        } catch (Exception e) {
            log.warn("Failed to update tool invocation: executionId={}, invocationId={}", executionId, id, e);
        }
    }

    private void updateExecutionCompleted(String status, long durationMs, String errorMessage) {
        try {
            String now = LocalDateTime.now().toString();
            executionRepository.save(AgentExecutionEntity.builder()
                    .id(executionId)
                    .agentSpecId(agentSpec.getId())
                    .status(status)
                    .endTime(now)
                    .durationMs(durationMs)
                    .errorMessage(errorMessage)
                    .updatedAt(now)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to update execution status: executionId={}", executionId, e);
        }
    }

    private void updateExecutionStatus(String status, String errorMessage) {
        try {
            executionRepository.updateStatus(executionId, status, errorMessage);
        } catch (Exception e) {
            log.warn("Failed to update execution status: executionId={}", executionId, e);
        }
    }

    private void emitStateChangedEvent() {
        emitEvent(AgentEventType.AGENT_STATE_CHANGED, Map.of(
                "state", state.name(),
                "agentId", agentSpec.getId()
        ));
    }

    private void emitEvent(AgentEventType eventType, Map<String, Object> data) {
        try {
            AgentEvent event = AgentEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .type(eventType)
                    .source(agentSpec.getId())
                    .data(data)
                    .timestamp(Instant.now())
                    .build();
            webSocketHandler.publishEvent(event);
        } catch (Exception e) {
            log.warn("Failed to emit event: executionId={}, type={}", executionId, eventType, e);
        }
    }

    private long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (text.length() + 3) / 4);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}