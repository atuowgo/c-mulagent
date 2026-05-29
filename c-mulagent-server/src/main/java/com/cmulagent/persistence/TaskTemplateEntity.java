package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateEntity {
    private String id;
    private String name;
    private String description;
    private String category;
    private String planTemplate;
    private String agentBindings;
    private String skillBindings;
    private String toolBindings;
    private String version;
    private Boolean enabled;
    private String createdAt;
    private String updatedAt;
}