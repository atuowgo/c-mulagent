package com.cmulagent.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    /** Default max entries per namespace before auto-trim */
    private static final int DEFAULT_MAX_ENTRIES = 50;
    /** Max character length for a single value before truncation in prompts */
    private static final int DEFAULT_MAX_VALUE_LENGTH = 4000;
    /** Default context window size in tokens (100k for Claude) */
    private static final long DEFAULT_MAX_TOKENS = 100_000;
    /** Number of most recent messages to keep during compaction */
    private static final int KEEP_LAST_N = 3;

    /** Global shared context (backward-compatible) */
    private final Map<String, Object> globalContext = new ConcurrentHashMap<>();

    /** Per-taskPlanId namespaced contexts */
    private final Map<String, Map<String, Object>> taskContexts = new ConcurrentHashMap<>();

    /** Per-execution message history for compaction */
    private final Map<String, List<String>> executionMessages = new ConcurrentHashMap<>();

    @Autowired
    private CompactionStrategy compactionStrategy;

    // ---- Global context (backward-compatible) ----

    public void put(String key, Object value) {
        globalContext.put(key, value);
    }

    public Object get(String key) {
        return globalContext.get(key);
    }

    public String getAsString(String key) {
        Object value = globalContext.get(key);
        return value != null ? value.toString() : null;
    }

    public Map<String, Object> getMap() {
        return Collections.unmodifiableMap(globalContext);
    }

    public void putAll(Map<String, Object> entries) {
        if (entries != null) {
            globalContext.putAll(entries);
        }
    }

    public Object remove(String key) {
        return globalContext.remove(key);
    }

    public boolean containsKey(String key) {
        return globalContext.containsKey(key);
    }

    public void clear() {
        globalContext.clear();
    }

    // ---- Task-scoped context ----

    /**
     * Put a key-value pair into the task-scoped context.
     * Auto-trims if exceeding max entries.
     */
    public void putTask(String taskPlanId, String key, Object value) {
        Map<String, Object> ctx = taskContexts.computeIfAbsent(taskPlanId, k -> new ConcurrentHashMap<>());
        ctx.put(key, value);
        if (ctx.size() > DEFAULT_MAX_ENTRIES) {
            // Remove oldest entry (approximation via LinkedHashMap key ordering)
            String oldestKey = ctx.keySet().iterator().next();
            ctx.remove(oldestKey);
            log.debug("Task context trimmed for {}: removed key '{}' (size={})", taskPlanId, oldestKey, ctx.size());
        }
    }

    public Object getTask(String taskPlanId, String key) {
        Map<String, Object> ctx = taskContexts.get(taskPlanId);
        return ctx != null ? ctx.get(key) : null;
    }

    public String getTaskAsString(String taskPlanId, String key) {
        Object value = getTask(taskPlanId, key);
        return value != null ? value.toString() : null;
    }

    /**
     * Get all context entries for a task plan, merged with global context.
     * Task-scoped values override global values on key conflict.
     */
    public Map<String, Object> getMergedMap(String taskPlanId) {
        Map<String, Object> merged = new LinkedHashMap<>(globalContext);
        Map<String, Object> taskCtx = taskContexts.get(taskPlanId);
        if (taskCtx != null) {
            merged.putAll(taskCtx);
        }
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Get task-scoped context as a compact string suitable for LLM prompts.
     * Truncates individual values exceeding max length.
     */
    public String getTaskContextString(String taskPlanId) {
        Map<String, Object> merged = getMergedMap(taskPlanId);
        if (merged.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Object> entry : merged.entrySet()) {
            if (count > 0) sb.append("\n");
            String valueStr = entry.getValue() != null ? entry.getValue().toString() : "null";
            if (valueStr.length() > DEFAULT_MAX_VALUE_LENGTH) {
                valueStr = valueStr.substring(0, DEFAULT_MAX_VALUE_LENGTH) + "...[truncated " +
                        (valueStr.length() - DEFAULT_MAX_VALUE_LENGTH) + " chars]";
            }
            sb.append(entry.getKey()).append(": ").append(valueStr);
            count++;
        }
        return sb.toString();
    }

    public void putAllTask(String taskPlanId, Map<String, Object> entries) {
        if (entries == null || entries.isEmpty()) return;
        Map<String, Object> ctx = taskContexts.computeIfAbsent(taskPlanId, k -> new ConcurrentHashMap<>());
        ctx.putAll(entries);
    }

    public Object removeTask(String taskPlanId, String key) {
        Map<String, Object> ctx = taskContexts.get(taskPlanId);
        return ctx != null ? ctx.remove(key) : null;
    }

    public boolean containsTaskKey(String taskPlanId, String key) {
        Map<String, Object> ctx = taskContexts.get(taskPlanId);
        return ctx != null && ctx.containsKey(key);
    }

    /**
     * Clear all context for a specific task plan.
     * Should be called after task completion to free memory.
     */
    public void clearTask(String taskPlanId) {
        Map<String, Object> removed = taskContexts.remove(taskPlanId);
        if (removed != null) {
            log.debug("Cleared task context for plan {}: {} entries removed", taskPlanId, removed.size());
        }
    }

    /**
     * Get the number of active task context namespaces.
     */
    public int getActiveTaskCount() {
        return taskContexts.size();
    }

    /**
     * Get the size of a task's context (number of entries).
     */
    public int getTaskSize(String taskPlanId) {
        Map<String, Object> ctx = taskContexts.get(taskPlanId);
        return ctx != null ? ctx.size() : 0;
    }

    // ---- Compaction and prompt caching ----

    /**
     * Add a message to an execution's history and check if compaction is needed.
     * Returns the compacted content if compaction occurred, null otherwise.
     */
    public String addMessageAndCheckCompaction(String executionId, String message, long maxTokens) {
        List<String> messages = executionMessages.computeIfAbsent(executionId, k -> new ArrayList<>());
        messages.add(message);

        long estimatedTokens = estimateTotalTokens(messages);
        long effectiveMax = maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS;

        if (compactionStrategy != null && compactionStrategy.shouldCompact(estimatedTokens, effectiveMax)) {
            String compacted = compactionStrategy.compact(messages, KEEP_LAST_N);
            log.info("Compaction triggered for execution {}: {} tokens -> compacted", executionId, estimatedTokens);
            return compacted;
        }
        return null;
    }

    /**
     * Estimate token count for a list of messages (rough: chars/4).
     */
    public long estimateTotalTokens(List<String> messages) {
        long total = 0;
        for (String msg : messages) {
            total += (msg.length() + 3) / 4;
        }
        return total;
    }

    /**
     * Get the message history for an execution.
     */
    public List<String> getExecutionMessages(String executionId) {
        return executionMessages.getOrDefault(executionId, List.of());
    }

    /**
     * Clear execution message history.
     */
    public void clearExecutionMessages(String executionId) {
        executionMessages.remove(executionId);
    }

    /**
     * Build a prompt cache marker for a given system prompt text.
     * Anthropic prompt caching: cache the system prompt to reduce costs.
     * The adapter uses this marker to set the cache_control field in the API request.
     */
    public String buildPromptCacheMarker(String systemPrompt) {
        return "CACHE_START:" + systemPrompt + ":CACHE_END";
    }
}