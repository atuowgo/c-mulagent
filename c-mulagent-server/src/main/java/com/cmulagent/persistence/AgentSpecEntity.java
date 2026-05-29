package com.cmulagent.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSpecEntity {
    private String id;
    private String name;
    private String description;
    private String role;
    private String capabilities;
    private String llmModel;
    private String systemPrompt;
    private Double temperature;
    private Integer maxTokens;
    private String config;
    private Boolean enabled;
    private String createdAt;
    private String updatedAt;
}