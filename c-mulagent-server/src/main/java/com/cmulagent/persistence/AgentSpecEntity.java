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
    private String role;
    private String baseUrl;
    private String model;
    private String apiKey;
    private String tools;
    private Integer maxSteps;
    private String outputFormat;
    private Boolean enabled;
    private String createdAt;
    private String updatedAt;
}