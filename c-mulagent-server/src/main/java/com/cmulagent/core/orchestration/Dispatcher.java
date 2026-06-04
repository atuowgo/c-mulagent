package com.cmulagent.core.orchestration;

import com.cmulagent.core.agent.AgentOrchestrator;
import com.cmulagent.event.AgentEvent;
import com.cmulagent.event.AgentEventType;
import com.cmulagent.event.AgentWebSocketHandler;
import com.cmulagent.persistence.SubtaskEntity;
import com.cmulagent.persistence.SubtaskRepository;
import com.cmulagent.persistence.TaskPlanEntity;
import com.cmulagent.persistence.TaskPlanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AgentOrchestrator agentOrchestrator;
    private final SubtaskRepository subtaskRepository;
    private final TaskPlanRepository taskPlanRepository;
    private final AgentWebSocketHandler webSocketHandler;

    private final Set<String> cancelledPlanIds = ConcurrentHashMap.newKeySet();

    public Dispatcher(AgentOrchestrator agentOrchestrator,
                      SubtaskRepository subtaskRepository,
                      TaskPlanRepository taskPlanRepository,
                      AgentWebSocketHandler webSocketHandler) {
        this.agentOrchestrator = agentOrchestrator;
        this.subtaskRepository = subtaskRepository;
        this.taskPlanRepository = taskPlanRepository;
        this.webSocketHandler = webSocketHandler;
    }

    public void dispatch(TaskPlan taskPlan) {
        if (taskPlan.getId() == null || taskPlan.getId().isBlank()) {
            taskPlan.setId(UUID.randomUUID().toString());
        }
        String now = nowStr();
        if (taskPlan.getCreatedAt() == null) {
            taskPlan.setCreatedAt(now);
        }
        taskPlan.setUpdatedAt(now);
        taskPlan.setStatus("RUNNING");

        log.info("Dispatching task plan {}: '{}' with {} subtasks",
                taskPlan.getId(), taskPlan.getName(),
                taskPlan.getSubtasks() != null ? taskPlan.getSubtasks().size() : 0);

        // Save task plan to DB
        TaskPlanEntity tpEntity = toTaskPlanEntity(taskPlan);
        taskPlanRepository.save(tpEntity);

        // Save all subtasks to DB
        if (taskPlan.getSubtasks() != null && !taskPlan.getSubtasks().isEmpty()) {
            List<SubtaskEntity> subtaskEntities = taskPlan.getSubtasks().stream()
                    .map(s -> toSubtaskEntity(s, taskPlan.getId()))
                    .collect(Collectors.toList());
            subtaskRepository.batchSave(subtaskEntities);
        }

        emitEvent(AgentEventType.TASK_PROGRESS, taskPlan.getId(),
                Map.of("taskPlanId", taskPlan.getId(), "status", "RUNNING",
                        "message", "Task plan dispatched", "subtaskCount",
                        taskPlan.getSubtasks() != null ? taskPlan.getSubtasks().size() : 0));

        // Find subtasks with no dependencies and execute them
        if (taskPlan.getSubtasks() != null) {
            List<SubtaskPlan> ready = taskPlan.getSubtasks().stream()
                    .filter(s -> s.getDependencies() == null || s.getDependencies().isEmpty())
                    .collect(Collectors.toList());

            for (SubtaskPlan subtask : ready) {
                executeSubtask(subtask, taskPlan);
            }

            // If no subtasks are ready (all have unmet dependencies), check for impossible state
            if (ready.isEmpty() && !taskPlan.getSubtasks().isEmpty()) {
                log.warn("No ready subtasks for plan {}, all have dependencies", taskPlan.getId());
            }
        }
    }

    private void executeSubtask(SubtaskPlan subtask, TaskPlan taskPlan) {
        if (cancelledPlanIds.contains(taskPlan.getId())) {
            log.info("Skipping subtask {} because plan {} was cancelled", subtask.getId(), taskPlan.getId());
            return;
        }

        String now = nowStr();
        subtask.setStatus("RUNNING");
        subtask.setStartedAt(now);
        subtask.setUpdatedAt(now);
        updateSubtaskInDb(subtask);

        log.info("Starting subtask {}:'{}' with agent {} (plan={})",
                subtask.getId(), subtask.getName(), subtask.getAssignedAgent(), taskPlan.getId());

        emitEvent(AgentEventType.TASK_PROGRESS, subtask.getId(),
                Map.of("taskPlanId", taskPlan.getId(), "subtaskId", subtask.getId(),
                        "status", "RUNNING", "subtaskName", subtask.getName(),
                        "assignedAgent", subtask.getAssignedAgent()));

        CompletableFuture<String> future = agentOrchestrator.executeWithAgent(
                subtask.getAssignedAgent(), subtask.getId(),
                subtask.getInputData() != null ? subtask.getInputData() : "");

        future.thenAccept(result -> {
            if (cancelledPlanIds.contains(taskPlan.getId())) {
                log.info("Ignoring subtask {} result, plan {} was cancelled", subtask.getId(), taskPlan.getId());
                return;
            }

            String completeNow = nowStr();
            subtask.setOutputData(result);
            subtask.setStatus("COMPLETED");
            subtask.setCompletedAt(completeNow);
            subtask.setUpdatedAt(completeNow);
            updateSubtaskInDb(subtask);

            log.info("Subtask {}:'{}' completed successfully", subtask.getId(), subtask.getName());

            emitEvent(AgentEventType.TASK_PROGRESS, subtask.getId(),
                    Map.of("taskPlanId", taskPlan.getId(), "subtaskId", subtask.getId(),
                            "status", "COMPLETED", "subtaskName", subtask.getName(),
                            "outputLength", result != null ? result.length() : 0));

            checkAndDispatchNext(taskPlan);
        }).exceptionally(ex -> {
            if (cancelledPlanIds.contains(taskPlan.getId())) {
                log.info("Ignoring subtask {} failure, plan {} was cancelled", subtask.getId(), taskPlan.getId());
                return null;
            }

            Throwable cause = ex != null ? ex.getCause() : null;
            String errorMsg = cause != null ? cause.getMessage() : (ex != null ? ex.getMessage() : "unknown error");
            log.error("Subtask {}:'{}' failed: {}", subtask.getId(), subtask.getName(), errorMsg);

            int retryCount = subtask.getRetryCount() != null ? subtask.getRetryCount() : 0;
            int maxRetries = subtask.getMaxRetries() != null ? subtask.getMaxRetries() : 3;

            if (retryCount < maxRetries) {
                subtask.setRetryCount(retryCount + 1);
                subtask.setStatus("PENDING");
                subtask.setUpdatedAt(nowStr());
                updateSubtaskInDb(subtask);

                log.info("Retrying subtask {} (attempt {}/{})", subtask.getId(),
                        subtask.getRetryCount(), maxRetries);

                emitEvent(AgentEventType.TASK_PROGRESS, subtask.getId(),
                        Map.of("taskPlanId", taskPlan.getId(), "subtaskId", subtask.getId(),
                                "status", "RETRYING", "subtaskName", subtask.getName(),
                                "retryCount", subtask.getRetryCount(),
                                "maxRetries", maxRetries,
                                "error", errorMsg != null ? errorMsg : "unknown error"));

                executeSubtask(subtask, taskPlan);
            } else {
                String failNow = nowStr();
                subtask.setOutputData(errorMsg);
                subtask.setStatus("FAILED");
                subtask.setCompletedAt(failNow);
                subtask.setUpdatedAt(failNow);
                updateSubtaskInDb(subtask);

                log.error("Subtask {}:'{}' failed permanently after {} retries",
                        subtask.getId(), subtask.getName(), maxRetries);

                emitEvent(AgentEventType.TASK_PROGRESS, subtask.getId(),
                        Map.of("taskPlanId", taskPlan.getId(), "subtaskId", subtask.getId(),
                                "status", "FAILED", "subtaskName", subtask.getName(),
                                "retryCount", retryCount, "maxRetries", maxRetries,
                                "error", errorMsg));

                checkAndDispatchNext(taskPlan);
            }
            return null;
        });
    }

    private void checkAndDispatchNext(TaskPlan taskPlan) {
        if (cancelledPlanIds.contains(taskPlan.getId())) {
            return;
        }

        List<SubtaskEntity> entities = subtaskRepository.findByTaskPlanId(taskPlan.getId());
        List<SubtaskPlan> allSubtasks = entities.stream()
                .map(this::toSubtaskPlan)
                .collect(Collectors.toList());

        // Check if all subtasks have reached terminal state
        boolean allTerminal = allSubtasks.stream().allMatch(s ->
                "COMPLETED".equals(s.getStatus())
                        || "FAILED".equals(s.getStatus())
                        || "CANCELLED".equals(s.getStatus()));

        if (allTerminal) {
            boolean hasFailed = allSubtasks.stream().anyMatch(s -> "FAILED".equals(s.getStatus()));
            String finalStatus = hasFailed ? "FAILED" : "COMPLETED";
            taskPlanRepository.updateStatus(taskPlan.getId(), finalStatus);

            log.info("Task plan {} finished with status: {}", taskPlan.getId(), finalStatus);

            emitEvent(AgentEventType.TASK_PROGRESS, taskPlan.getId(),
                    Map.of("taskPlanId", taskPlan.getId(), "status", finalStatus,
                            "message", "Task plan " + finalStatus.toLowerCase()));
            return;
        }

        // Find subtasks whose dependencies are now all met
        for (SubtaskPlan subtask : allSubtasks) {
            if (!"PENDING".equals(subtask.getStatus())) {
                continue;
            }
            if (subtask.getDependencies() == null || subtask.getDependencies().isEmpty()) {
                continue;
            }

            boolean allDepsMet = subtask.getDependencies().stream().allMatch(depName ->
                    allSubtasks.stream().anyMatch(s ->
                            s.getName().equals(depName) && "COMPLETED".equals(s.getStatus())));

            if (allDepsMet) {
                // Collect outputs from completed dependencies
                StringBuilder inputBuilder = new StringBuilder();
                if (subtask.getInputData() != null && !subtask.getInputData().isBlank()) {
                    inputBuilder.append(subtask.getInputData());
                }
                for (String depName : subtask.getDependencies()) {
                    allSubtasks.stream()
                            .filter(s -> s.getName().equals(depName) && "COMPLETED".equals(s.getStatus()))
                            .findFirst()
                            .ifPresent(dep -> {
                                if (dep.getOutputData() != null && !dep.getOutputData().isBlank()) {
                                    inputBuilder.append("\n\n--- Output from '")
                                            .append(depName).append("' ---\n")
                                            .append(dep.getOutputData());
                                }
                            });
                }
                subtask.setInputData(inputBuilder.toString());

                log.info("Dependencies met for subtask {}:'{}', dispatching",
                        subtask.getId(), subtask.getName());
                executeSubtask(subtask, taskPlan);
            }
        }
    }

    public void cancel(String taskPlanId) {
        log.info("Cancelling task plan {}", taskPlanId);
        cancelledPlanIds.add(taskPlanId);

        taskPlanRepository.updateStatus(taskPlanId, "CANCELLED");

        List<SubtaskEntity> entities = subtaskRepository.findByTaskPlanId(taskPlanId);
        for (SubtaskEntity e : entities) {
            if ("PENDING".equals(e.getStatus()) || "RUNNING".equals(e.getStatus())) {
                subtaskRepository.updateStatus(e.getId(), "CANCELLED");
                log.info("Cancelled subtask {}:'{}'", e.getId(), e.getName());
            }
        }

        emitEvent(AgentEventType.TASK_PROGRESS, taskPlanId,
                Map.of("taskPlanId", taskPlanId, "status", "CANCELLED",
                        "message", "Task plan cancelled"));
    }

    private void updateSubtaskInDb(SubtaskPlan subtask) {
        SubtaskEntity entity = toSubtaskEntity(subtask, subtask.getTaskPlanId());
        subtaskRepository.save(entity);
    }

    private void emitEvent(AgentEventType type, String source, Map<String, Object> data) {
        AgentEvent event = AgentEvent.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .source(source)
                .data(data)
                .timestamp(Instant.now())
                .build();
        webSocketHandler.publishEvent(event);
    }

    private String nowStr() {
        return LocalDateTime.now().toString();
    }

    private TaskPlanEntity toTaskPlanEntity(TaskPlan plan) {
        return TaskPlanEntity.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .status(plan.getStatus())
                .priority(plan.getPriority())
                .parentId(null)
                .context(null)
                .metadata(null)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .completedAt(plan.getCompletedAt())
                .build();
    }

    private SubtaskEntity toSubtaskEntity(SubtaskPlan plan, String taskPlanId) {
        String depsJson = "[]";
        if (plan.getDependencies() != null && !plan.getDependencies().isEmpty()) {
            try {
                depsJson = mapper.writeValueAsString(plan.getDependencies());
            } catch (Exception e) {
                log.warn("Failed to serialize dependencies for subtask {}", plan.getId(), e);
            }
        }

        return SubtaskEntity.builder()
                .id(plan.getId())
                .taskPlanId(taskPlanId)
                .name(plan.getName())
                .description(plan.getDescription())
                .status(plan.getStatus())
                .assignedAgent(plan.getAssignedAgent())
                .inputData(plan.getInputData())
                .outputData(plan.getOutputData())
                .priority(plan.getPriority())
                .dependencies(depsJson)
                .retryCount(plan.getRetryCount())
                .maxRetries(plan.getMaxRetries())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .startedAt(plan.getStartedAt())
                .completedAt(plan.getCompletedAt())
                .build();
    }

    private SubtaskPlan toSubtaskPlan(SubtaskEntity entity) {
        List<String> deps = new ArrayList<>();
        if (entity.getDependencies() != null && !entity.getDependencies().isBlank()) {
            try {
                deps = mapper.readValue(entity.getDependencies(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse dependencies JSON for subtask {}: {}", entity.getId(), e.getMessage());
            }
        }

        return SubtaskPlan.builder()
                .id(entity.getId())
                .taskPlanId(entity.getTaskPlanId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .assignedAgent(entity.getAssignedAgent())
                .inputData(entity.getInputData())
                .outputData(entity.getOutputData())
                .priority(entity.getPriority())
                .dependencies(deps)
                .retryCount(entity.getRetryCount())
                .maxRetries(entity.getMaxRetries())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}