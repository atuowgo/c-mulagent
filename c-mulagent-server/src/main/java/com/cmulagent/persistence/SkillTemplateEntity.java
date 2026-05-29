package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTemplateEntity {
    private String id;
    private String name;
    private String description;
    private String category;
    private String promptTemplate;
    private String toolBindings;
    private String inputSchema;
    private String outputSchema;
    private String version;
    private Boolean enabled;
    private String createdAt;
    private String updatedAt;
}