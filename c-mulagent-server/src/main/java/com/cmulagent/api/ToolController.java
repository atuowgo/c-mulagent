package com.cmulagent.api;

import com.cmulagent.core.tool.ToolExecutionException;
import com.cmulagent.core.tool.ToolRegistry;
import com.cmulagent.core.tool.ToolSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private static final Logger log = LoggerFactory.getLogger(ToolController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> registerTool(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String name = Objects.toString(request.get("name"), null);
            if (name == null || name.isBlank()) {
                return errorResponse(HttpStatus.BAD_REQUEST, "Tool name is required");
            }

            String description = Objects.toString(request.get("description"), "");
            @SuppressWarnings("unchecked")
            Map<String, Object> inputSchema = request.get("inputSchema") instanceof Map
                    ? objectMapper.convertValue(request.get("inputSchema"), Map.class) : null;

            ToolSpec spec = ToolSpec.builder()
                    .name(name)
                    .description(description)
                    .inputSchema(inputSchema)
                    .build();

            toolRegistry.register(name, spec, params -> {
                log.debug("Invoking API-registered tool '{}' with params: {}", name, params);
                try {
                    return objectMapper.writeValueAsString(params);
                } catch (Exception e) {
                    return "{\"error\":\"Failed to serialize params\"}";
                }
            });

            log.info("Tool registered: {}", name);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", name);
            data.put("description", description);
            data.put("inputSchema", inputSchema);
            return successResponse(HttpStatus.CREATED, data);
        }).onErrorResume(e -> {
            log.error("Failed to register tool", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> listTools() {
        return Mono.fromCallable(() -> {
            log.info("Listing all tools");
            List<ToolSpec> specs = toolRegistry.getSpecs();
            List<Map<String, Object>> items = specs.stream().map(spec -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", spec.getName());
                m.put("description", spec.getDescription());
                m.put("inputSchema", spec.getInputSchema());
                m.put("category", spec.getCategory());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            data.put("total", items.size());
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to list tools", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/{name}")
    public Mono<ResponseEntity<Map<String, Object>>> getTool(@PathVariable String name) {
        return Mono.fromCallable(() -> {
            log.info("Fetching tool: {}", name);
            return toolRegistry.getSpec(name)
                    .map(spec -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name", spec.getName());
                        m.put("description", spec.getDescription());
                        m.put("inputSchema", spec.getInputSchema());
                        m.put("category", spec.getCategory());
                        return successResponse(m);
                    })
                    .orElseGet(() -> errorResponse(HttpStatus.NOT_FOUND, "Tool not found: " + name));
        }).onErrorResume(e -> {
            log.error("Failed to fetch tool: {}", name, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @PostMapping("/{name}/invoke")
    public Mono<ResponseEntity<Map<String, Object>>> invokeTool(@PathVariable String name,
                                                                  @RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            log.info("Invoking tool: {}", name);
            try {
                String result = toolRegistry.invoke(name, request);

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("toolName", name);
                data.put("params", request);
                data.put("result", result);
                return successResponse(data);
            } catch (ToolExecutionException e) {
                log.warn("Tool execution failed: {} - {}", name, e.getMessage());
                return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }).onErrorResume(e -> {
            log.error("Failed to invoke tool: {}", name, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
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