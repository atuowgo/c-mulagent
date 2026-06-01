package com.cmulagent.api;

import com.cmulagent.persistence.TaskTemplateEntity;
import com.cmulagent.persistence.TaskTemplateRepository;
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
@RequestMapping("/api/templates")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TaskTemplateRepository taskTemplateRepository;

    public TemplateController(TaskTemplateRepository taskTemplateRepository) {
        this.taskTemplateRepository = taskTemplateRepository;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createTemplate(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Creating template: {}", request.get("name"));
            String id = UUID.randomUUID().toString();
            String now = LocalDateTime.now().toString();

            TaskTemplateEntity entity = TaskTemplateEntity.builder()
                    .id(id)
                    .name(Objects.toString(request.get("name"), "Unnamed Template"))
                    .description(Objects.toString(request.get("description"), ""))
                    .category(Objects.toString(request.get("category"), null))
                    .planTemplate(Objects.toString(request.get("planTemplate"), ""))
                    .agentBindings(toJson(request.get("agentBindings")))
                    .skillBindings(toJson(request.get("skillBindings")))
                    .toolBindings(toJson(request.get("toolBindings")))
                    .version(Objects.toString(request.get("version"), "1.0.0"))
                    .enabled(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            taskTemplateRepository.save(entity);

            log.info("Template created: id={}", id);
            return successResponse(HttpStatus.CREATED, templateEntityToMap(entity));
        }).onErrorResume(e -> {
            log.error("Failed to create template", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getTemplate(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Fetching template: {}", id);
            Optional<TaskTemplateEntity> entityOpt = taskTemplateRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Template not found: " + id);
            }
            return successResponse(templateEntityToMap(entityOpt.get()));
        }).onErrorResume(e -> {
            log.error("Failed to fetch template: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> listTemplates() {
        return Mono.fromCallable(() -> {
            log.info("Listing all templates");
            List<TaskTemplateEntity> entities = taskTemplateRepository.findAll();
            List<Map<String, Object>> items = entities.stream()
                    .map(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", e.getId());
                        item.put("name", e.getName());
                        item.put("description", e.getDescription());
                        item.put("category", e.getCategory());
                        item.put("version", e.getVersion());
                        item.put("enabled", e.getEnabled());
                        item.put("createdAt", e.getCreatedAt());
                        item.put("updatedAt", e.getUpdatedAt());
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("total", items.size());
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to list templates", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateTemplate(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Updating template: {}", id);
            Optional<TaskTemplateEntity> entityOpt = taskTemplateRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Template not found: " + id);
            }
            TaskTemplateEntity existing = entityOpt.get();
            String now = LocalDateTime.now().toString();

            if (request.containsKey("name")) existing.setName(Objects.toString(request.get("name"), existing.getName()));
            if (request.containsKey("description")) existing.setDescription(Objects.toString(request.get("description"), existing.getDescription()));
            if (request.containsKey("category")) existing.setCategory(Objects.toString(request.get("category"), existing.getCategory()));
            if (request.containsKey("planTemplate")) existing.setPlanTemplate(Objects.toString(request.get("planTemplate"), existing.getPlanTemplate()));
            if (request.containsKey("version")) existing.setVersion(Objects.toString(request.get("version"), existing.getVersion()));
            if (request.containsKey("enabled") && request.get("enabled") instanceof Boolean b) existing.setEnabled(b);
            if (request.containsKey("agentBindings")) existing.setAgentBindings(toJson(request.get("agentBindings")));
            if (request.containsKey("skillBindings")) existing.setSkillBindings(toJson(request.get("skillBindings")));
            if (request.containsKey("toolBindings")) existing.setToolBindings(toJson(request.get("toolBindings")));
            existing.setUpdatedAt(now);

            taskTemplateRepository.save(existing);

            log.info("Template updated: {}", id);
            return successResponse(templateEntityToMap(existing));
        }).onErrorResume(e -> {
            log.error("Failed to update template: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteTemplate(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Deleting template: {}", id);
            Optional<TaskTemplateEntity> entityOpt = taskTemplateRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Template not found: " + id);
            }
            taskTemplateRepository.deleteById(id);
            log.info("Template deleted: {}", id);
            return successResponse(Map.of("id", id, "deleted", true));
        }).onErrorResume(e -> {
            log.error("Failed to delete template: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    private Map<String, Object> templateEntityToMap(TaskTemplateEntity e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("name", e.getName());
        map.put("description", e.getDescription());
        map.put("category", e.getCategory());
        map.put("planTemplate", e.getPlanTemplate());
        map.put("version", e.getVersion());
        map.put("enabled", e.getEnabled());
        map.put("agentBindings", parseJson(e.getAgentBindings()));
        map.put("skillBindings", parseJson(e.getSkillBindings()));
        map.put("toolBindings", parseJson(e.getToolBindings()));
        map.put("createdAt", e.getCreatedAt());
        map.put("updatedAt", e.getUpdatedAt());
        return map;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize to JSON: {}", value, e);
            return null;
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return json;
        }
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