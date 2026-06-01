package com.cmulagent.core.orchestration;

import com.cmulagent.core.agent.AgentOrchestrator;
import com.cmulagent.llm.LLMClient;
import com.cmulagent.persistence.AgentSpecEntity;
import com.cmulagent.persistence.AgentSpecRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class Aggregator {

    private static final Logger log = LoggerFactory.getLogger(Aggregator.class);

    private final AgentOrchestrator agentOrchestrator;
    private final AgentSpecRepository agentSpecRepository;

    public Aggregator(AgentOrchestrator agentOrchestrator,
                      AgentSpecRepository agentSpecRepository) {
        this.agentOrchestrator = agentOrchestrator;
        this.agentSpecRepository = agentSpecRepository;
    }

    public String aggregate(List<String> results, String agentSpecId) {
        if (results == null || results.isEmpty()) {
            log.warn("No results to aggregate");
            return "";
        }

        if (results.size() == 1) {
            log.info("Single result, returning directly");
            return results.get(0);
        }

        log.info("Aggregating {} results using agent spec {}", results.size(), agentSpecId);

        AgentSpecEntity spec = agentSpecRepository.findById(agentSpecId)
                .orElseThrow(() -> new IllegalArgumentException("Agent spec not found: " + agentSpecId));

        LLMClient llmClient = agentOrchestrator.createLLMClient(spec);

        String systemPrompt = buildAggregationSystemPrompt();
        String userMessage = buildAggregationUserMessage(results);

        try {
            String aggregated = llmClient.chat(systemPrompt,
                            List.of(new LLMClient.Message("user", userMessage)))
                    .get(120, TimeUnit.SECONDS);
            log.info("Aggregation complete, result length: {}",
                    aggregated != null ? aggregated.length() : 0);
            return aggregated;
        } catch (Exception e) {
            log.error("Aggregation LLM call failed", e);
            throw new RuntimeException("Result aggregation failed: " + e.getMessage(), e);
        }
    }

    public String aggregateWithTemplate(List<String> results, String template) {
        if (results == null || results.isEmpty()) {
            log.warn("No results to aggregate with template");
            return template != null ? template : "";
        }

        if (template == null || template.isBlank()) {
            log.warn("No template provided, joining results with newlines");
            return String.join("\n\n", results);
        }

        log.info("Aggregating {} results with template", results.size());

        String result = template;
        for (int i = 0; i < results.size(); i++) {
            String placeholder = "{{result_" + i + "}}";
            result = result.replace(placeholder,
                    results.get(i) != null ? results.get(i) : "");
        }

        // Remove any remaining unresolved placeholders
        result = result.replaceAll("\\{\\{result_\\d+\\}\\}", "");

        return result;
    }

    private String buildAggregationSystemPrompt() {
        return """
                You are a result aggregation expert. Your job is to combine and synthesize
                multiple subtask results into a coherent, comprehensive final output.

                Guidelines:
                1. Identify and remove duplicate information across results
                2. Organize content logically with clear structure
                3. Preserve all important findings and conclusions
                4. Resolve any contradictions by noting differing viewpoints
                5. Maintain a professional, clear tone
                6. Include all key details without excessive verbosity

                Output the final synthesized result directly, without any preamble or meta-commentary.
                """;
    }

    private String buildAggregationUserMessage(List<String> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please synthesize the following ").append(results.size())
                .append(" subtask results into one cohesive output:\n\n");

        for (int i = 0; i < results.size(); i++) {
            sb.append("=== Result ").append(i + 1).append(" ===\n");
            sb.append(results.get(i));
            sb.append("\n\n");
        }

        sb.append("Produce the final synthesized result now.");
        return sb.toString();
    }
}