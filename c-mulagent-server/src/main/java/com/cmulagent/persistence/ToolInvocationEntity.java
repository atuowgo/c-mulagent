package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInvocationEntity {
    private String id;
    private String agentExecutionId;
    private String toolName;
    private String inputParams;
    private String outputResult;
    private String status;
    private Long durationMs;
    private String errorMessage;
    private String createdAt;
    private String completedAt;
}