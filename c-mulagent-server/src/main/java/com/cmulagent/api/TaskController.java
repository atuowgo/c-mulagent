package com.cmulagent.api;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @PostMapping
    public Mono<Map<String, Object>> createTask(@RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getTask(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping
    public Mono<Map<String, Object>> listTasks() {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PostMapping("/{id}/start")
    public Mono<Map<String, Object>> startTask(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PostMapping("/{id}/cancel")
    public Mono<Map<String, Object>> cancelTask(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/{id}/progress")
    public Mono<Map<String, Object>> getTaskProgress(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }
}