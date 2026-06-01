package com.cmulagent.core.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, ToolExecutor> tools = new HashMap<>();
    private final Map<String, ToolSpec> specs = new HashMap<>();

    public void register(String name, ToolExecutor executor) {
        register(name, ToolSpec.builder().name(name).build(), executor);
    }

    public void register(String name, ToolSpec spec, ToolExecutor executor) {
        tools.put(name, executor);
        specs.put(name, spec);
        log.info("Tool registered: {}", name);
    }

    public ToolExecutor get(String name) {
        return tools.get(name);
    }

    public Optional<ToolSpec> getSpec(String name) {
        return Optional.ofNullable(specs.get(name));
    }

    public List<String> getAll() {
        List<String> names = new ArrayList<>(tools.keySet());
        Collections.sort(names);
        return names;
    }

    public List<ToolSpec> getSpecs() {
        return new ArrayList<>(specs.values());
    }

    public String invoke(String name, Map<String, Object> params) {
        ToolExecutor executor = tools.get(name);
        if (executor == null) {
            log.error("Tool not found for invocation: {}", name);
            throw new ToolExecutionException(name, "Tool not registered: " + name);
        }
        log.debug("Invoking tool: {}", name);
        try {
            String result = executor.execute(params);
            log.debug("Tool {} returned: {}", name, result);
            return result;
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", name, e.getMessage());
            throw new ToolExecutionException(name, "Tool execution failed: " + e.getMessage(), e);
        }
    }

    public void unregister(String name) {
        tools.remove(name);
        specs.remove(name);
        log.info("Tool unregistered: {}", name);
    }
}