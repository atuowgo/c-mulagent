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
    private String name;
    private String description;
    private String assignedAgent;
    private int priority;
    private List<String> dependencies;
}