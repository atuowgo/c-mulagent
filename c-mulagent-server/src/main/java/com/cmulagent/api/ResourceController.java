package com.cmulagent.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

    @GetMapping("/slots")
    public Mono<ResponseEntity<Map<String, Object>>> listResourceSlots() {
        return Mono.fromCallable(() -> {
            log.info("Listing resource slots");
            List<Map<String, Object>> slots = IntStream.range(0, 4).mapToObj(i -> {
                Map<String, Object> slot = new LinkedHashMap<>();
                slot.put("id", "slot-" + (i + 1));
                slot.put("name", "Resource Slot " + (i + 1));
                slot.put("status", i < 2 ? "IDLE" : "BUSY");
                slot.put("assignedAgent", i < 2 ? null : "agent-" + (i + 1));
                slot.put("maxConcurrency", 1);
                slot.put("currentLoad", i < 2 ? 0 : 1);
                return slot;
            }).collect(Collectors.toList());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", slots);
            data.put("total", slots.size());
            data.put("availableSlots", slots.stream().filter(s -> "IDLE".equals(s.get("status"))).count());
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Failed to list resource slots", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/slots/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> getResourceSlot(@PathVariable String id) {
        return Mono.fromCallable(() -> {
            log.info("Fetching resource slot: {}", id);
            int index = -1;
            try {
                index = Integer.parseInt(id.replace("slot-", ""));
            } catch (NumberFormatException ignored) {
            }
            if (index < 1 || index > 4) {
                return errorResponse(HttpStatus.NOT_FOUND, "Resource slot not found: " + id);
            }

            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("id", id);
            slot.put("name", "Resource Slot " + index);
            slot.put("status", index <= 2 ? "IDLE" : "BUSY");
            slot.put("assignedAgent", index <= 2 ? null : "agent-" + index);
            slot.put("maxConcurrency", 1);
            slot.put("currentLoad", index <= 2 ? 0 : 1);
            return successResponse(slot);
        }).onErrorResume(e -> {
            log.error("Failed to fetch resource slot: {}", id, e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return Mono.fromCallable(() -> {
            log.info("Health check requested");
            String uptime = getUptime();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "UP");
            data.put("timestamp", LocalDateTime.now().toString());
            data.put("uptime", uptime);
            data.put("services", Map.of(
                    "database", "UP",
                    "agents", "UP",
                    "tools", "UP"
            ));
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Health check failed", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    private String getUptime() {
        long uptimeMs = System.nanoTime() / 1_000_000;
        long hours = uptimeMs / 3_600_000;
        long minutes = (uptimeMs % 3_600_000) / 60_000;
        long seconds = (uptimeMs % 60_000) / 1000;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
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