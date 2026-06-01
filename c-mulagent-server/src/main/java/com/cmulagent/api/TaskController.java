package com.cmulagent.api;

import com.cmulagent.core.orchestration.Dispatcher;
import com.cmulagent.core.orchestration.SubtaskPlan;
import com.cmulagent.core.orchestration.TaskDecomposer;
import com.cmulagent.core.orchestration.TaskPlan;
import com.cmulagent.persistence.SubtaskEntity;
import com.cmulagent.persistence.SubtaskRepository;
import com.cmulagent.persistence.TaskPlanEntity;
import com.cmulagent.persistence.TaskPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TaskPlanRepository taskPlanRepository;
    private final SubtaskRepository subtaskRepository;
    private final TaskDecomposer taskDecomposer;
    private final Dispatcher dispatcher;

    public TaskController(TaskPlanRepository taskPlanRepository,
                          SubtaskRepository subtaskRepository,
                          TaskDecomposer taskDecomposer,
                          Dispatcher dispatcher) {
        this.taskPlanRepository = taskPlanRepository;
        this.subtaskRepository = subtaskRepository;
        this.taskDecomposer = taskDecomposer;
        this.dispatcher = dispatcher;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createTask(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Creating task: {}", request.get("name"));

            String id = UUID.randomUUID().toString();
            String now = LocalDateTime.now().toString();
            String name = Objects.toString(request.get("name"), "Untitled Task");
            String description = Objects.toString(request.get("description"), "");
            int priority = request.get("priority") instanceof Number n ? n.intValue() : 5;
            String context = Objects.toString(request.get("context"), null);
            String metadata = Objects.toString(request.get("metadata"), null);

            TaskPlanEntity entity = TaskPlanEntity.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .status("CREATED")
                    .priority(priority)
                    .context(context)
                    .metadata(metadata)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            taskPlanRepository.save(entity);

            List<SubtaskPlan> subtaskPlans = taskDecomposer.decompose(description);
            List<SubtaskEntity> subtaskEntities = new ArrayList<>();
            for (int i = 0; i < subtaskPlans.size(); i++) {
                SubtaskPlan sp = subtaskPlans.get(i);
                SubtaskEntity se = SubtaskEntity.builder()
                        .id(sp.getId() != null ? sp.getId() : UUID.randomUUID().toString())
                        .taskPlanId(id)
                        .name(sp.getName() != null ? sp.getName() : "Subtask " + (i + 1))
                        .description(sp.getDescription())
                        .status("PENDING")
                        .assignedAgent(sp.getAssignedAgent())
                        .inputData(sp.getInputData())
                        .priority(sp.getPriority() != null ? sp.getPriority() : i)
                        .dependencies(sp.getDependencies() != null
                                ? String.join(",", sp.getDependencies()) : null)
                        .retryCount(0)
                        .maxRetries(sp.getMaxRetries() != null ? sp.getMaxRetries() : 3)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                subtaskEntities.add(se);
            }

            if (!subtaskEntities.isEmpty()) {
                subtaskRepository.batchSave(subtaskEntities);
                entity.setStatus("READY");
                taskPlanRepository.updateStatus(id, "READY");
            }

            TaskPlan taskPlan = toDomain(entity, subtaskEntities);
            dispatcher.dispatch(taskPlan);
            taskPlanRepository.updateStatus(id, "RUNNING");
            entity.setStatus("RUNNING");

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", id);
            data.put("name", name);
            data.put("status", "RUNNING");
            data.put("subtaskCount", subtaskEntities.size());
            data.put("createdAt", now);

            log.info("Task created: id={}, subtasks={}", id, subtaskEntities.size());
            return successResponse(HttpStatus.CREATED, data);
        }).onErrorResume(e -> {
            log.error("Failed to create task", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getTask(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Fetching task: {}", id);
            Optional<TaskPlanEntity> entityOpt = taskPlanRepository.findById(id);
            if (entityOpt.isEmpty()) {
                log.warn("Task not found: {}", id);
                return errorResponse(HttpStatus.NOT_FOUND, "Task not found: " + id);
            }
            TaskPlanEntity entity = entityOpt.get();
            List<SubtaskEntity> subtasks = subtaskRepository.findByTaskPlanId(id);
            TaskPlan taskPlan = toDomain(entity, subtasks);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", entity.getId());
            data.put("name", entity.getName());
            data.put("description", entity.getDescription());
            data.put("status", entity.getStatus());
            data.put("priority", entity.getPriority());
            data.put("context", entity.getContext());
            data.put("metadata", entity.getMetadata());
            data.put("createdAt", entity.getCreatedAt());
            data.put("updatedAt", entity.getUpdatedAt());
            data.put("completedAt", entity.getCompletedAt());
            data.put("subtasks", subtasks.stream().map(this::subtaskToMap).collect(Collectors.toList()));

            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to fetch task: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> listTasks() {
        return Mono.fromCallable(() -> {
            log.info("Listing all tasks");
            List<TaskPlanEntity> entities = taskPlanRepository.findAll();
            List<Map<String, Object>> items = entities.stream().map(e -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", e.getId());
                item.put("name", e.getName());
                item.put("status", e.getStatus());
                item.put("priority", e.getPriority());
                item.put("createdAt", e.getCreatedAt());
                item.put("updatedAt", e.getUpdatedAt());
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("total", items.size());
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to list tasks", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PostMapping("/{id}/start")
    public Mono<ResponseEntity<Map<String, Object>>> startTask(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Starting task: {}", id);
            Optional<TaskPlanEntity> entityOpt = taskPlanRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Task not found: " + id);
            }
            TaskPlanEntity entity = entityOpt.get();
            List<SubtaskEntity> subtasks = subtaskRepository.findByTaskPlanId(id);
            TaskPlan taskPlan = toDomain(entity, subtasks);

            dispatcher.dispatch(taskPlan);
            taskPlanRepository.updateStatus(id, "RUNNING");

            log.info("Task started: {}", id);
            return successResponse(Map.of("id", id, "status", "RUNNING"));
        }).onErrorResume(e -> {
            log.error("Failed to start task: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PostMapping("/{id}/cancel")
    public Mono<ResponseEntity<Map<String, Object>>> cancelTask(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Cancelling task: {}", id);
            Optional<TaskPlanEntity> entityOpt = taskPlanRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Task not found: " + id);
            }
            dispatcher.cancel(id);
            taskPlanRepository.updateStatus(id, "CANCELLED");

            log.info("Task cancelled: {}", id);
            return successResponse(Map.of("id", id, "status", "CANCELLED"));
        }).onErrorResume(e -> {
            log.error("Failed to cancel task: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/{id}/progress")
    public Mono<ResponseEntity<Map<String, Object>>> getTaskProgress(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Fetching task progress: {}", id);
            Optional<TaskPlanEntity> entityOpt = taskPlanRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Task not found: " + id);
            }
            TaskPlanEntity entity = entityOpt.get();
            List<SubtaskEntity> subtasks = subtaskRepository.findByTaskPlanId(id);

            Map<String, Long> statusCounts = subtasks.stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getStatus() != null ? s.getStatus() : "UNKNOWN",
                            Collectors.counting()));

            long total = subtasks.size();
            long completed = statusCounts.getOrDefault("COMPLETED", 0L);
            double progress = total > 0 ? (double) completed / total * 100.0 : 0.0;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("taskId", id);
            data.put("taskStatus", entity.getStatus());
            data.put("totalSubtasks", total);
            data.put("statusCounts", statusCounts);
            data.put("progress", Math.round(progress * 10.0) / 10.0);
            data.put("subtasks", subtasks.stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("status", s.getStatus());
                m.put("assignedAgent", s.getAssignedAgent());
                return m;
            }).collect(Collectors.toList()));

            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to fetch task progress: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    private TaskPlan toDomain(TaskPlanEntity entity, List<SubtaskEntity> subtaskEntities) {
        return TaskPlan.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .subtasks(subtaskEntities.stream().map(se -> SubtaskPlan.builder()
                        .id(se.getId())
                        .taskPlanId(se.getTaskPlanId())
                        .name(se.getName())
                        .description(se.getDescription())
                        .status(se.getStatus())
                        .assignedAgent(se.getAssignedAgent())
                        .inputData(se.getInputData())
                        .outputData(se.getOutputData())
                        .priority(se.getPriority())
                        .dependencies(se.getDependencies() != null && !se.getDependencies().isBlank()
                                ? Arrays.asList(se.getDependencies().split(",")) : List.of())
                        .retryCount(se.getRetryCount())
                        .maxRetries(se.getMaxRetries())
                        .createdAt(se.getCreatedAt())
                        .updatedAt(se.getUpdatedAt())
                        .startedAt(se.getStartedAt())
                        .completedAt(se.getCompletedAt())
                        .build()).collect(Collectors.toList()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    private Map<String, Object> subtaskToMap(SubtaskEntity se) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", se.getId());
        map.put("name", se.getName());
        map.put("description", se.getDescription());
        map.put("status", se.getStatus());
        map.put("assignedAgent", se.getAssignedAgent());
        map.put("priority", se.getPriority());
        map.put("retryCount", se.getRetryCount());
        map.put("maxRetries", se.getMaxRetries());
        map.put("dependencies", se.getDependencies());
        map.put("createdAt", se.getCreatedAt());
        map.put("updatedAt", se.getUpdatedAt());
        map.put("startedAt", se.getStartedAt());
        map.put("completedAt", se.getCompletedAt());
        return map;
    }

    private ResponseEntity<Map<String, Object>> successResponse(Object data) {
        return successResponse(HttpStatus.OK, data);
    }

    private ResponseEntity<Map<String, Object>> successResponse(HttpStatus status, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("error", null);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String error) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("data", null);
        response.put("error", error);
        return ResponseEntity.status(status).body(response);
    }
}