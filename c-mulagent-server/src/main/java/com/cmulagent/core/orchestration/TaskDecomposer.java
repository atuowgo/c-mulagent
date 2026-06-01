package com.cmulagent.core.orchestration;

import com.cmulagent.core.agent.AgentOrchestrator;
import com.cmulagent.llm.LLMClient;
import com.cmulagent.persistence.AgentSpecEntity;
import com.cmulagent.persistence.AgentSpecRepository;
import com.cmulagent.persistence.TaskTemplateEntity;
import com.cmulagent.persistence.TaskTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TaskDecomposer {

    private static final Logger log = LoggerFactory.getLogger(TaskDecomposer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n(\\{.*?\\})\\s*\\n```", Pattern.DOTALL);

    private final AgentOrchestrator agentOrchestrator;
    private final AgentSpecRepository agentSpecRepository;
    private final TaskTemplateRepository taskTemplateRepository;

    public TaskDecomposer(AgentOrchestrator agentOrchestrator,
                          AgentSpecRepository agentSpecRepository,
                          TaskTemplateRepository taskTemplateRepository) {
        this.agentOrchestrator = agentOrchestrator;
        this.agentSpecRepository = agentSpecRepository;
        this.taskTemplateRepository = taskTemplateRepository;
    }

    public List<SubtaskPlan> decompose(String taskDescription) {
        List<AgentSpecEntity> specs = agentSpecRepository.findAll();
        if (specs.isEmpty()) {
            log.info("No agent specs configured, creating simple single-subtask decomposition");
            return simpleDecomposition(taskDescription);
        }
        AgentSpecEntity best = specs.stream()
                .filter(s -> s.getEnabled() != null && s.getEnabled())
                .findFirst().orElse(specs.get(0));
        return decompose(taskDescription, best.getId());
    }

    public List<SubtaskPlan> decompose(String taskDescription, String agentSpecId) {
        log.info("Decomposing task with agent spec {}", agentSpecId);

        AgentSpecEntity spec = agentSpecRepository.findById(agentSpecId)
                .orElseThrow(() -> new IllegalArgumentException("Agent spec not found: " + agentSpecId));

        LLMClient llmClient = agentOrchestrator.createLLMClient(spec);

        String systemPrompt = buildDecompositionSystemPrompt();
        List<LLMClient.Message> messages = List.of(
                new LLMClient.Message("user", taskDescription)
        );

        String response;
        try {
            response = llmClient.chat(systemPrompt, messages).get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Task decomposition LLM call failed", e);
            throw new RuntimeException("Task decomposition failed: " + e.getMessage(), e);
        }

        String taskPlanId = UUID.randomUUID().toString();
        List<SubtaskPlan> subtasks = parseDecompositionResponse(response, taskPlanId);

        log.info("Task decomposed into {} subtasks (planId={})", subtasks.size(), taskPlanId);
        return subtasks;
    }

    public List<SubtaskPlan> decomposeWithTemplate(String taskDescription, String templateId) {
        log.info("Decomposing task with template {}", templateId);

        TaskTemplateEntity template = taskTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Task template not found: " + templateId));

        String planTemplate = template.getPlanTemplate();
        if (planTemplate == null || planTemplate.isBlank()) {
            throw new IllegalArgumentException("Task template " + templateId + " has no planTemplate");
        }

        String filled = planTemplate.replace("{{description}}", taskDescription);
        // Replace any remaining {{variable}} placeholders with empty string
        filled = filled.replaceAll("\\{\\{[^}]+\\}\\}", "");

        String taskPlanId = UUID.randomUUID().toString();
        List<SubtaskPlan> subtasks = parseDecompositionResponse(filled, taskPlanId);

        log.info("Task decomposed from template into {} subtasks (planId={})", subtasks.size(), taskPlanId);
        return subtasks;
    }

    private String buildDecompositionSystemPrompt() {
        return """
                You are a task decomposition expert. Your job is to break down a complex task description
                into a sequence of subtasks that can be executed by specialized AI agents.

                Analyze the task and determine:
                1. What are the logical steps required to complete this task?
                2. What dependencies exist between steps (which steps must complete before others can start)?
                3. What type of agent would be best suited for each step?

                Output ONLY valid JSON in the following format. Do not include any other text:
                {
                  "planName": "A concise name for the overall plan",
                  "subtasks": [
                    {
                      "name": "Short name for the subtask",
                      "description": "Detailed description of what this subtask should accomplish",
                      "assignedAgent": "Agent spec name or role that best fits this subtask",
                      "dependencies": ["subtask_id_of_dependency"],
                      "priority": 0
                    }
                  ]
                }

                Rules:
                - Each subtask must have a unique name
                - dependencies should list the names of subtasks that must complete first (use subtask names, not IDs)
                - priority: 0 = highest, larger numbers = lower priority
                - Order subtasks logically: independent tasks first, dependent tasks after
                - Keep the number of subtasks reasonable (2-7)
                - assignedAgent should be a descriptive role name (e.g. "researcher", "coder", "analyst")
                """;
    }

    private List<SubtaskPlan> parseDecompositionResponse(String response, String taskPlanId) {
        String json = extractJSON(response);
        log.debug("Extracted JSON for decomposition: {}", json);

        try {
            Map<String, Object> root = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subtaskMaps = (List<Map<String, Object>>) root.getOrDefault("subtasks", List.of());

            String now = LocalDateTime.now().toString();
            List<SubtaskPlan> subtasks = new ArrayList<>();

            for (int i = 0; i < subtaskMaps.size(); i++) {
                Map<String, Object> sm = subtaskMaps.get(i);
                String subtaskId = UUID.randomUUID().toString();

                @SuppressWarnings("unchecked")
                List<String> dependencies = sm.containsKey("dependencies") && sm.get("dependencies") != null
                        ? (List<String>) sm.get("dependencies")
                        : List.of();

                SubtaskPlan plan = SubtaskPlan.builder()
                        .id(subtaskId)
                        .taskPlanId(taskPlanId)
                        .name(getString(sm, "name", "subtask-" + i))
                        .description(getString(sm, "description", ""))
                        .status("PENDING")
                        .assignedAgent(getString(sm, "assignedAgent", ""))
                        .inputData(getString(sm, "description", ""))
                        .outputData(null)
                        .priority(getInt(sm, "priority", i))
                        .dependencies(dependencies)
                        .retryCount(0)
                        .maxRetries(3)
                        .createdAt(now)
                        .updatedAt(now)
                        .startedAt(null)
                        .completedAt(null)
                        .build();

                subtasks.add(plan);
            }

            return subtasks;
        } catch (Exception e) {
            log.error("Failed to parse decomposition response: {}", json, e);
            throw new RuntimeException("Failed to parse decomposition response: " + e.getMessage(), e);
        }
    }

    private String extractJSON(String response) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private List<SubtaskPlan> simpleDecomposition(String taskDescription) {
        String taskPlanId = UUID.randomUUID().toString();
        String now = LocalDateTime.now().toString();
        SubtaskPlan plan = SubtaskPlan.builder()
                .id(UUID.randomUUID().toString())
                .taskPlanId(taskPlanId)
                .name("Execute Task")
                .description(taskDescription)
                .status("PENDING")
                .priority(0)
                .dependencies(List.of())
                .retryCount(0)
                .maxRetries(3)
                .createdAt(now)
                .updatedAt(now)
                .build();
        log.info("Simple decomposition: 1 subtask (planId={})", taskPlanId);
        return List.of(plan);
    }
}