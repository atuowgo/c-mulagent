package com.cmulagent.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnthropicAdapter implements LLMClient {

    private final String apiKey;
    private final String model;

    public AnthropicAdapter(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        // TODO: implement Anthropic API call via anthropic-java SDK
        return CompletableFuture.completedFuture("");
    }
}