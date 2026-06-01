package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionEntity {
    private String id;
    private String subtaskId;
    private String agentSpecId;
    private String status;
    private String startTime;
    private String endTime;
    private Long durationMs;
    private Long totalTokens;
    private String errorMessage;
    private String metadata;
    private String createdAt;
    private String updatedAt;
}