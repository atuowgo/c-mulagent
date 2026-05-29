package com.cmulagent.api;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @PostMapping
    public Mono<Map<String, Object>> createAgent(@RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getAgent(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping
    public Mono<Map<String, Object>> listAgents() {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PutMapping("/{id}")
    public Mono<Map<String, Object>> updateAgent(@PathVariable String id, @RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @DeleteMapping("/{id}")
    public Mono<Map<String, Object>> deleteAgent(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PostMapping("/{id}/test")
    public Mono<Map<String, Object>> testAgent(@PathVariable String id, @RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }
}