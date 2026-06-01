package com.cmulagent.core.orchestration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskPlan {
    private String id;
    private String taskPlanId;
    private String name;
    private String description;
    private String status;
    private String assignedAgent;
    private String inputData;
    private String outputData;
    private Integer priority;
    private Integer retryCount;
    private Integer maxRetries;
    private List<String> dependencies;
    private String createdAt;
    private String updatedAt;
    private String startedAt;
    private String completedAt;
}