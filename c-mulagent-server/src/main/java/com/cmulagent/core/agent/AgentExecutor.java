package com.cmulagent.core.agent;

import com.cmulagent.llm.LLMClient;

public class AgentExecutor {

    private final AgentSpec agentSpec;
    private final LLMClient llmClient;

    public AgentExecutor(AgentSpec agentSpec, LLMClient llmClient) {
        this.agentSpec = agentSpec;
        this.llmClient = llmClient;
    }

    public String execute(String input) {
        // TODO: implement agent execution logic
        return "";
    }
}