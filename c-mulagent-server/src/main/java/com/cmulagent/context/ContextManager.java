package com.cmulagent.context;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContextManager {

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public String getAsString(String key) {
        Object value = context.get(key);
        return value != null ? value.toString() : null;
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(context);
    }

    public void putAll(Map<String, Object> entries) {
        if (entries != null) {
            context.putAll(entries);
        }
    }

    public Object remove(String key) {
        return context.remove(key);
    }

    public boolean containsKey(String key) {
        return context.containsKey(key);
    }

    public void clear() {
        context.clear();
    }
}