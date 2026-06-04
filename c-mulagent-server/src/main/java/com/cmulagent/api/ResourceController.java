package com.cmulagent.api;

import com.cmulagent.resource.EndpointLoadBalancer;
import com.cmulagent.resource.ResourceSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);

    private final EndpointLoadBalancer loadBalancer;

    public ResourceController(EndpointLoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @GetMapping("/slots")
    public Mono<ResponseEntity<Map<String, Object>>> listResourceSlots() {
        return Mono.fromCallable(() -> {
            log.info("Listing resource slots");
            List<ResourceSlot> allSlots = loadBalancer.getAllSlots();
            List<Map<String, Object>> slotMaps = new ArrayList<>();
            for (ResourceSlot slot : allSlots) {
                slotMaps.add(slotToMap(slot));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", slotMaps);
            data.put("total", slotMaps.size());
            data.put("availableSlots", allSlots.stream().filter(ResourceSlot::isAvailable).count());
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
            ResourceSlot slot = loadBalancer.getSlot(id);
            if (slot == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Resource slot not found: " + id);
            }
            return successResponse(slotToMap(slot));
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

            List<ResourceSlot> allSlots = loadBalancer.getAllSlots();
            long availableCount = allSlots.stream().filter(ResourceSlot::isAvailable).count();
            int totalCapacity = allSlots.stream().mapToInt(ResourceSlot::getMaxCapacity).sum();
            int totalUsed = allSlots.stream().mapToInt(ResourceSlot::getUsedCapacity).sum();

            Map<String, Object> services = new LinkedHashMap<>();
            services.put("database", "UP");
            services.put("agents", "UP");
            services.put("tools", "UP");
            services.put("loadBalancer", Map.of(
                    "status", "UP",
                    "totalSlots", allSlots.size(),
                    "availableSlots", availableCount,
                    "totalCapacity", totalCapacity,
                    "totalUsed", totalUsed
            ));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "UP");
            data.put("timestamp", LocalDateTime.now().toString());
            data.put("uptime", uptime);
            data.put("services", services);
            return successResponse(data);
        }).onErrorResume(e -> {
            log.error("Health check failed", e);
            return Mono.just(errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        });
    }

    private Map<String, Object> slotToMap(ResourceSlot slot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", slot.getId());
        map.put("name", slot.getName());
        map.put("type", slot.getType());
        map.put("maxCapacity", slot.getMaxCapacity());
        map.put("usedCapacity", slot.getUsedCapacity());
        map.put("available", slot.isAvailable());
        return map;
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