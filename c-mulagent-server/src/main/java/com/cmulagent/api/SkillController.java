package com.cmulagent.api;

import com.cmulagent.core.skill.SkillLoader;
import com.cmulagent.core.skill.SkillTemplate;
import com.cmulagent.persistence.SkillTemplateEntity;
import com.cmulagent.persistence.SkillTemplateRepository;
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
@RequestMapping("/api/skills")
public class SkillController {

    private static final Logger log = LoggerFactory.getLogger(SkillController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SkillTemplateRepository skillTemplateRepository;
    private final SkillLoader skillLoader;

    public SkillController(SkillTemplateRepository skillTemplateRepository, SkillLoader skillLoader) {
        this.skillTemplateRepository = skillTemplateRepository;
        this.skillLoader = skillLoader;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createSkill(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Creating skill: {}", request.get("name"));
            String id = UUID.randomUUID().toString();
            String now = LocalDateTime.now().toString();

            SkillTemplateEntity entity = SkillTemplateEntity.builder()
                    .id(id)
                    .name(Objects.toString(request.get("name"), "Unnamed Skill"))
                    .description(Objects.toString(request.get("description"), ""))
                    .category(Objects.toString(request.get("category"), null))
                    .promptTemplate(Objects.toString(request.get("promptTemplate"), ""))
                    .toolBindings(toJson(request.get("toolBindings")))
                    .inputSchema(toJson(request.get("inputSchema")))
                    .outputSchema(toJson(request.get("outputSchema")))
                    .version(Objects.toString(request.get("version"), "1.0.0"))
                    .enabled(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            skillTemplateRepository.save(entity);

            log.info("Skill created: id={}", id);
            return successResponse(HttpStatus.CREATED, skillEntityToMap(entity));
        }).onErrorResume(e -> {
            log.error("Failed to create skill", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getSkill(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Fetching skill: {}", id);
            Optional<SkillTemplateEntity> entityOpt = skillTemplateRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Skill not found: " + id);
            }
            return successResponse(skillEntityToMap(entityOpt.get()));
        }).onErrorResume(e -> {
            log.error("Failed to fetch skill: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> listSkills() {
        return Mono.fromCallable(() -> {
            log.info("Listing all skills");
            List<SkillTemplate> skills = skillLoader.loadAll();
            List<Map<String, Object>> items = skills.stream()
                    .map(this::skillDomainToMap)
                    .collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("total", items.size());
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to list skills", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateSkill(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Updating skill: {}", id);
            Optional<SkillTemplateEntity> entityOpt = skillTemplateRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Skill not found: " + id);
            }
            SkillTemplateEntity existing = entityOpt.get();
            String now = LocalDateTime.now().toString();

            if (request.containsKey("name")) existing.setName(Objects.toString(request.get("name"), existing.getName()));
            if (request.containsKey("description")) existing.setDescription(Objects.toString(request.get("description"), existing.getDescription()));
            if (request.containsKey("category")) existing.setCategory(Objects.toString(request.get("category"), existing.getCategory()));
            if (request.containsKey("promptTemplate")) existing.setPromptTemplate(Objects.toString(request.get("promptTemplate"), existing.getPromptTemplate()));
            if (request.containsKey("version")) existing.setVersion(Objects.toString(request.get("version"), existing.getVersion()));
            if (request.containsKey("enabled") && request.get("enabled") instanceof Boolean b) existing.setEnabled(b);
            if (request.containsKey("toolBindings")) existing.setToolBindings(toJson(request.get("toolBindings")));
            if (request.containsKey("inputSchema")) existing.setInputSchema(toJson(request.get("inputSchema")));
            if (request.containsKey("outputSchema")) existing.setOutputSchema(toJson(request.get("outputSchema")));
            existing.setUpdatedAt(now);

            skillTemplateRepository.save(existing);

            log.info("Skill updated: {}", id);
            return successResponse(skillEntityToMap(existing));
        }).onErrorResume(e -> {
            log.error("Failed to update skill: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteSkill(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Deleting skill: {}", id);
            Optional<SkillTemplateEntity> entityOpt = skillTemplateRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return errorResponse(HttpStatus.NOT_FOUND, "Skill not found: " + id);
            }
            skillTemplateRepository.deleteById(id);
            log.info("Skill deleted: {}", id);
            return successResponse(Map.of("id", id, "deleted", true));
        }).onErrorResume(e -> {
            log.error("Failed to delete skill: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    private Map<String, Object> skillEntityToMap(SkillTemplateEntity e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("name", e.getName());
        map.put("description", e.getDescription());
        map.put("category", e.getCategory());
        map.put("promptTemplate", e.getPromptTemplate());
        map.put("version", e.getVersion());
        map.put("enabled", e.getEnabled());
        map.put("toolBindings", parseJson(e.getToolBindings()));
        map.put("inputSchema", parseJson(e.getInputSchema()));
        map.put("outputSchema", parseJson(e.getOutputSchema()));
        map.put("createdAt", e.getCreatedAt());
        map.put("updatedAt", e.getUpdatedAt());
        return map;
    }

    private Map<String, Object> skillDomainToMap(SkillTemplate s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("name", s.getName());
        map.put("description", s.getDescription());
        map.put("category", s.getCategory());
        map.put("version", s.getVersion());
        map.put("enabled", s.getEnabled());
        map.put("toolBindings", s.getToolBindings());
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