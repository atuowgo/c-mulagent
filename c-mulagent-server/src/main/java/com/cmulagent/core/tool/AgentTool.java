package com.cmulagent.core.tool;

import com.cmulagent.core.agent.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Built-in tool for delegating tasks to a sub-agent.
 * This is the core mechanism for tree-structured orchestration.
 */
public class AgentTool {

    private static final Logger log = LoggerFactory.getLogger(AgentTool.class);

    private static final long DEFAULT_TIMEOUT_MINUTES = 10;

    private final AgentOrchestrator orchestrator;

    public AgentTool(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public ToolExecutor executor() {
        return params -> {
            try {
                String agentName = (String) params.get("agentName");
                String prompt = (String) params.get("prompt");

                if (agentName == null || agentName.isBlank()) {
                    return "Error: 'agentName' parameter is required";
                }
                if (prompt == null || prompt.isBlank()) {
                    return "Error: 'prompt' parameter is required";
                }

                String subtaskId = "subtask-" + UUID.randomUUID().toString().substring(0, 8);

                log.info("Delegating to agent '{}' with subtaskId: {}", agentName, subtaskId);

                CompletableFuture<String> future = orchestrator.executeWithAgent(agentName, subtaskId, prompt);

                String result = future.get(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                log.info("Sub-agent '{}' (subtaskId: {}) completed", agentName, subtaskId);
                return result;

            } catch (java.util.concurrent.TimeoutException e) {
                log.error("Sub-agent timed out: {}", params.get("agentName"));
                return "Error: Sub-agent execution timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes";
            } catch (IllegalArgumentException e) {
                return "Error: " + e.getMessage();
            } catch (Exception e) {
                log.error("Sub-agent execution failed", e);
                return "Error executing sub-agent: " + e.getMessage();
            }
        };
    }
}