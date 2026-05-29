package com.cmulagent.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LLMClient {

    CompletableFuture<String> chat(String systemPrompt, List<Message> messages);

    record Message(String role, String content) {}
}