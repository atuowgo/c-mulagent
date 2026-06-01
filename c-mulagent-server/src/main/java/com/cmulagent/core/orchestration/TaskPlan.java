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
public class TaskPlan {
    private String id;
    private String name;
    private String description;
    private String status;
    private Integer priority;
    private List<SubtaskPlan> subtasks;
    private String createdAt;
    private String updatedAt;
    private String completedAt;
}