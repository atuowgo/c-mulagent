package com.cmulagent.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OllamaAdapter implements LLMClient {

    private final String endpoint;
    private final String model;

    public OllamaAdapter(String endpoint, String model) {
        this.endpoint = endpoint;
        this.model = model;
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        // TODO: implement Ollama API call via OkHttp
        return CompletableFuture.completedFuture("");
    }
}