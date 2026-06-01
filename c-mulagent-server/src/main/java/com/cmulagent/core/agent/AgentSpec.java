package com.cmulagent.core.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSpec {
    private String id;
    private String name;
    private String role;
    private String baseUrl;
    private String model;
    private String apiKey;
    private List<String> tools;
    private Integer maxSteps;
    private String outputFormat;
    private Boolean enabled;
}