package com.cmulagent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAiCompatAdapter implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatAdapter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public OpenAiCompatAdapter(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint + "/v1/chat/completions";
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.executor = Executors.newFixedThreadPool(4);
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        log.debug("OpenAI-compat request: endpoint={}, model={}, messageCount={}", endpoint, model, messages.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, String>> msgList = new ArrayList<>(messages.size() + 1);
                msgList.add(Map.of("role", "system", "content", systemPrompt));
                for (LLMClient.Message msg : messages) {
                    msgList.add(Map.of("role", msg.role(), "content", msg.content()));
                }

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", model);
                body.put("messages", msgList);
                body.put("max_tokens", 4096);
                body.put("temperature", 0.7);

                String json = mapper.writeValueAsString(body);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .timeout(Duration.ofSeconds(120))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("OpenAI-compat API error: status={}, body={}", response.statusCode(), response.body());
                    throw new RuntimeException("OpenAI-compat API returned status " + response.statusCode() + ": " + response.body());
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> respMap = mapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new RuntimeException("OpenAI-compat API returned empty choices");
                }
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                log.debug("OpenAI-compat response: length={}", content != null ? content.length() : 0);
                return content;
            } catch (Exception e) {
                log.error("OpenAI-compat API call failed: endpoint={}, model={}", endpoint, model, e);
                throw new RuntimeException("OpenAI-compat API call failed: " + e.getMessage(), e);
            }
        }, executor);
    }
}