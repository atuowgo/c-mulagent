package com.cmulagent.core.tool;

import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, ToolExecutor> tools = new HashMap<>();

    public void register(String name, ToolExecutor executor) {
        tools.put(name, executor);
    }

    public ToolExecutor get(String name) {
        return tools.get(name);
    }
}