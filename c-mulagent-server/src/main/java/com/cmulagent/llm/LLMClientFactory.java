package com.cmulagent.llm;

import org.springframework.stereotype.Component;

@Component
public class LLMClientFactory {

    public LLMClient createAnthropic(String apiKey, String model) {
        return new AnthropicAdapter(apiKey, model);
    }

    public LLMClient createOpenAiCompat(String endpoint, String apiKey, String model) {
        return new OpenAiCompatAdapter(endpoint, apiKey, model);
    }

    public LLMClient createOllama(String endpoint, String model) {
        return new OllamaAdapter(endpoint, model);
    }
}