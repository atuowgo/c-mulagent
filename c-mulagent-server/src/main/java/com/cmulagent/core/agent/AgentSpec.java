package com.cmulagent.core.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSpec {
    private String id;
    private String name;
    private String description;
    private String role;
    private String llmModel;
    private String systemPrompt;
    private double temperature;
    private int maxTokens;
}