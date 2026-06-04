package com.cmulagent.context;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ThresholdCompactionStrategy implements CompactionStrategy {

    private static final double THRESHOLD = 0.8;

    @Override
    public boolean shouldCompact(long estimatedTokens, long maxTokens) {
        return estimatedTokens > (long)(maxTokens * THRESHOLD);
    }

    @Override
    public String compact(List<String> messages, int keepLastN) {
        if (messages.size() <= keepLastN) {
            return String.join("\n", messages);
        }
        int splitPoint = messages.size() - keepLastN;
        List<String> toSummarize = messages.subList(0, splitPoint);
        List<String> toKeep = messages.subList(splitPoint, messages.size());

        StringBuilder summary = new StringBuilder();
        summary.append("[Summary of earlier conversation (").append(toSummarize.size()).append(" messages)]\n");
        for (String msg : toSummarize) {
            String truncated = msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
            summary.append("- ").append(truncated).append("\n");
        }
        summary.append("\n[Recent messages]\n");
        for (String msg : toKeep) {
            summary.append(msg).append("\n");
        }
        return summary.toString();
    }
}