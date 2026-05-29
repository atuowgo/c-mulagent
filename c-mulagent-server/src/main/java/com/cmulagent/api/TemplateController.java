package com.cmulagent.api;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @PostMapping
    public Mono<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getTemplate(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping
    public Mono<Map<String, Object>> listTemplates() {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PutMapping("/{id}")
    public Mono<Map<String, Object>> updateTemplate(@PathVariable String id, @RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @DeleteMapping("/{id}")
    public Mono<Map<String, Object>> deleteTemplate(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }
}