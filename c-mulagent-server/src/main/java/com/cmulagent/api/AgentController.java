package com.cmulagent.api;

import com.cmulagent.core.agent.AgentOrchestrator;
import com.cmulagent.persistence.AgentSpecEntity;
import com.cmulagent.persistence.AgentSpecRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
@RequestMapping("/api/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentSpecRepository agentSpecRepository;
    private final AgentOrchestrator agentOrchestrator;

    public AgentController(AgentSpecRepository agentSpecRepository,
                           AgentOrchestrator agentOrchestrator) {
        this.agentSpecRepository = agentSpecRepository;
        this.agentOrchestrator = agentOrchestrator;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> createAgent(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Creating agent: {}", request.get("name"));

            String id = UUID.randomUUID().toString();
            String now = LocalDateTime.now().toString();

            String toolsJson = "[]";
            Object toolsObj = request.get("tools");
            if (toolsObj instanceof List<?> list && !list.isEmpty()) {
                toolsJson = objectMapper.writeValueAsString(list);
            }

            AgentSpecEntity entity = AgentSpecEntity.builder()
                    .id(id)
                    .name(Objects.toString(request.get("name"), "Unnamed Agent"))
                    .role(Objects.toString(request.get("role"), ""))
                    .baseUrl(Objects.toString(request.get("baseUrl"), null))
                    .model(Objects.toString(request.get("model"), null))
                    .apiKey(Objects.toString(request.get("apiKey"), null))
                    .tools(toolsJson)
                    .maxSteps(request.get("maxSteps") instanceof Number n ? n.intValue() : 10)
                    .outputFormat(Objects.toString(request.get("outputFormat"), null))
                    .enabled(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            agentSpecRepository.save(entity);

            log.info("Agent created: id={}, name={}", id, entity.getName());
            return successResponse(HttpStatus.CREATED, agentEntityToMap(entity));
        }).onErrorResume(e -> {
            log.error("Failed to create agent", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getAgent(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Fetching agent: {}", id);
            Optional<AgentSpecEntity> entityOpt = agentSpecRepository.findById(id);
            if (entityOpt.isEmpty()) {
                log.warn("Agent not found: {}", id);
                return errorResponse(HttpStatus.NOT_FOUND, "Agent not found: " + id);
            }
            return successResponse(agentEntityToMap(entityOpt.get()));
        }).onErrorResume(e -> {
            log.error("Failed to fetch agent: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> listAgents() {
        return Mono.fromCallable(() -> {
            log.info("Listing all agents");
            List<AgentSpecEntity> entities = agentSpecRepository.findAll();
            List<Map<String, Object>> items = entities.stream()
                    .map(this::agentEntityToBrief)
                    .collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("total", items.size());
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to list agents", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> updateAgent(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Updating agent: {}", id);
            Optional<AgentSpecEntity> existingOpt = agentSpecRepository.findById(id);
            if (existingOpt.isEmpty()) {
                log.warn("Agent not found for update: {}", id);
                return errorResponse(HttpStatus.NOT_FOUND, "Agent not found: " + id);
            }

            AgentSpecEntity existing = existingOpt.get();
            String now = LocalDateTime.now().toString();

            if (request.containsKey("name")) existing.setName(Objects.toString(request.get("name"), existing.getName()));
            if (request.containsKey("role")) existing.setRole(Objects.toString(request.get("role"), existing.getRole()));
            if (request.containsKey("baseUrl")) existing.setBaseUrl(Objects.toString(request.get("baseUrl"), existing.getBaseUrl()));
            if (request.containsKey("model")) existing.setModel(Objects.toString(request.get("model"), existing.getModel()));
            if (request.containsKey("apiKey")) existing.setApiKey(Objects.toString(request.get("apiKey"), existing.getApiKey()));
            if (request.containsKey("maxSteps") && request.get("maxSteps") instanceof Number n) existing.setMaxSteps(n.intValue());
            if (request.containsKey("outputFormat")) existing.setOutputFormat(Objects.toString(request.get("outputFormat"), existing.getOutputFormat()));
            if (request.containsKey("enabled") && request.get("enabled") instanceof Boolean b) existing.setEnabled(b);
            if (request.containsKey("tools") && request.get("tools") instanceof List<?> list) {
                existing.setTools(objectMapper.writeValueAsString(list));
            }
            existing.setUpdatedAt(now);

            agentSpecRepository.save(existing);

            log.info("Agent updated: id={}, name={}", id, existing.getName());
            return successResponse(agentEntityToMap(existing));
        }).onErrorResume(e -> {
            log.error("Failed to update agent: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteAgent(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Deleting agent: {}", id);
            Optional<AgentSpecEntity> entityOpt = agentSpecRepository.findById(id);
            if (entityOpt.isEmpty()) {
                log.warn("Agent not found for delete: {}", id);
                return errorResponse(HttpStatus.NOT_FOUND, "Agent not found: " + id);
            }
            agentSpecRepository.deleteById(id);
            log.info("Agent deleted: {}", id);
            return successResponse(Map.of("id", id, "deleted", true));
        }).onErrorResume(e -> {
            log.error("Failed to delete agent: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PostMapping("/{id}/test")
    public Mono<ResponseEntity<Map<String, Object>>> testAgent(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Testing agent: {}", id);
            Optional<AgentSpecEntity> entityOpt = agentSpecRepository.findById(id);
            if (entityOpt.isEmpty()) {
                log.warn("Agent not found for test: {}", id);
                return errorResponse(HttpStatus.NOT_FOUND, "Agent not found: " + id);
            }

            AgentSpecEntity entity = entityOpt.get();
            if (entity.getEnabled() == null || !entity.getEnabled()) {
                return errorResponse(HttpStatus.BAD_REQUEST, "Agent is disabled");
            }

            String input = Objects.toString(request.get("input"), "Hello, introduce yourself briefly.");
            String subtaskId = "test-" + UUID.randomUUID().toString().substring(0, 8);

            try {
                String result = agentOrchestrator.executeWithAgent(id, subtaskId, input).get();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("agentId", id);
                data.put("agentName", entity.getName());
                data.put("input", input);
                data.put("output", result);
                data.put("subtaskId", subtaskId);
                return successResponse(data);
            } catch (Exception e) {
                log.error("Agent test execution failed: id={}", id, e);
                return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Agent test failed: " + e.getMessage());
            }
        }).onErrorResume(e -> {
            log.error("Failed to test agent: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    private Map<String, Object> agentEntityToMap(AgentSpecEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("name", entity.getName());
        map.put("role", entity.getRole());
        map.put("baseUrl", entity.getBaseUrl());
        map.put("model", entity.getModel());
        map.put("apiKey", maskApiKey(entity.getApiKey()));
        map.put("tools", parseTools(entity.getTools()));
        map.put("maxSteps", entity.getMaxSteps());
        map.put("outputFormat", entity.getOutputFormat());
        map.put("enabled", entity.getEnabled());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }

    private Map<String, Object> agentEntityToBrief(AgentSpecEntity e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("name", e.getName());
        map.put("role", e.getRole());
        map.put("model", e.getModel());
        map.put("enabled", e.getEnabled());
        map.put("updatedAt", e.getUpdatedAt());
        return map;
    }

    private List<String> parseTools(String toolsJson) {
        if (toolsJson == null || toolsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(toolsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse tools JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) return apiKey;
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
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