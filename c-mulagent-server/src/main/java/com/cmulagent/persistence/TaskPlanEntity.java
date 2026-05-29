package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlanEntity {
    private String id;
    private String name;
    private String description;
    private String status;
    private Integer priority;
    private String parentId;
    private String context;
    private String metadata;
    private String createdAt;
    private String updatedAt;
    private String completedAt;
}