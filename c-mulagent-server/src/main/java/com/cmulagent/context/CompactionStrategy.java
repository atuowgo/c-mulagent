package com.cmulagent.context;

import java.util.List;

public interface CompactionStrategy {
    /**
     * Check if compaction should be triggered.
     * @param estimatedTokens current estimated token count
     * @param maxTokens the context window size
     * @return true if compaction is needed
     */
    boolean shouldCompact(long estimatedTokens, long maxTokens);

    /**
     * Compact a list of messages into a summary.
     * @param messages the messages to compact (oldest to newest)
     * @param keepLastN number of most recent messages to keep uncompacted
     * @return a summary string of the compacted messages
     */
    String compact(List<String> messages, int keepLastN);
}