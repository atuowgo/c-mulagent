package com.cmulagent.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnthropicAdapter implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicAdapter.class);

    private final AnthropicClient client;
    private final String model;
    private final ExecutorService executor;

    public AnthropicAdapter(String apiKey, String model) {
        this.model = model;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        log.debug("Anthropic request: model={}, systemPrompt={}, messageCount={}", model, systemPrompt, messages.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<MessageParam> messageParams = new ArrayList<>(messages.size());
                for (LLMClient.Message msg : messages) {
                    MessageParam.Role role = "assistant".equals(msg.role())
                            ? MessageParam.Role.ASSISTANT
                            : MessageParam.Role.USER;
                    messageParams.add(MessageParam.builder()
                            .role(role)
                            .content(msg.content())
                            .build());
                }

                MessageCreateParams params = MessageCreateParams.builder()
                        .model(model)
                        .system(systemPrompt)
                        .messages(messageParams)
                        .maxTokens(4096)
                        .build();

                var response = client.messages().create(params);

                StringBuilder sb = new StringBuilder();
                for (var block : response.content()) {
                    if (block.isText()) {
                        sb.append(block.asText().text());
                    }
                }
                String content = sb.toString();
                log.debug("Anthropic response: length={}", content.length());
                return content;
            } catch (Exception e) {
                log.error("Anthropic API call failed: model={}", model, e);
                throw new RuntimeException("Anthropic API call failed: " + e.getMessage(), e);
            }
        }, executor);
    }
}