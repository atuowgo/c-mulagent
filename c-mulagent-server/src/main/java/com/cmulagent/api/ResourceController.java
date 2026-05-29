package com.cmulagent.api;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @GetMapping("/slots")
    public Mono<Map<String, Object>> listResourceSlots() {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/slots/{id}")
    public Mono<Map<String, Object>> getResourceSlot(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/health")
    public Mono<Map<String, Object>> healthCheck() {
        // TODO: implement
        return Mono.just(Map.of());
    }
}