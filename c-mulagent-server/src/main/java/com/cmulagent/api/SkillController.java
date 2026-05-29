package com.cmulagent.api;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    @PostMapping
    public Mono<Map<String, Object>> createSkill(@RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/{id}")
    public Mono<Map<String, Object>> getSkill(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping
    public Mono<Map<String, Object>> listSkills() {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PutMapping("/{id}")
    public Mono<Map<String, Object>> updateSkill(@PathVariable String id, @RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @DeleteMapping("/{id}")
    public Mono<Map<String, Object>> deleteSkill(@PathVariable String id) {
        // TODO: implement
        return Mono.just(Map.of());
    }
}