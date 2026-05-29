package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskEntity {
    private String id;
    private String taskPlanId;
    private String name;
    private String description;
    private String status;
    private String assignedAgent;
    private String inputData;
    private String outputData;
    private Integer priority;
    private String dependencies;
    private Integer retryCount;
    private Integer maxRetries;
    private String createdAt;
    private String updatedAt;
    private String startedAt;
    private String completedAt;
}