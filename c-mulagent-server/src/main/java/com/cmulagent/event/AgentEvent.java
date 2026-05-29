package com.cmulagent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent {
    private String id;
    private AgentEventType type;
    private String source;
    private Map<String, Object> data;
    private Instant timestamp;
}