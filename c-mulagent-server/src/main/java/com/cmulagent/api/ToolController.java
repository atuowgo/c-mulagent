package com.cmulagent.api;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    @PostMapping
    public Mono<Map<String, Object>> registerTool(@RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping
    public Mono<Map<String, Object>> listTools() {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @GetMapping("/{name}")
    public Mono<Map<String, Object>> getTool(@PathVariable String name) {
        // TODO: implement
        return Mono.just(Map.of());
    }

    @PostMapping("/{name}/invoke")
    public Mono<Map<String, Object>> invokeTool(@PathVariable String name, @RequestBody Map<String, Object> request) {
        // TODO: implement
        return Mono.just(Map.of());
    }
}