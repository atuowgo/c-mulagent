package com.cmulagent.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAiCompatAdapter implements LLMClient {

    private final String endpoint;
    private final String apiKey;
    private final String model;

    public OpenAiCompatAdapter(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        // TODO: implement OpenAI-compatible API call via OkHttp
        return CompletableFuture.completedFuture("");
    }
}