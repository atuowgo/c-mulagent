package com.cmulagent.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextManager {

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public void clear() {
        context.clear();
    }
}