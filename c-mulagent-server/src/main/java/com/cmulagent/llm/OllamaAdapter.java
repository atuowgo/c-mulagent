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

public class OllamaAdapter implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaAdapter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String endpoint;
    private final String model;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public OllamaAdapter(String endpoint, String model) {
        this.endpoint = endpoint + "/api/chat";
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.executor = Executors.newFixedThreadPool(4);
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        log.debug("Ollama request: endpoint={}, model={}, messageCount={}", endpoint, model, messages.size());

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
                body.put("stream", false);
                body.put("options", Map.of("temperature", 0.7));

                String json = mapper.writeValueAsString(body);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .timeout(Duration.ofSeconds(120))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("Ollama API error: status={}, body={}", response.statusCode(), response.body());
                    throw new RuntimeException("Ollama API returned status " + response.statusCode() + ": " + response.body());
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> respMap = mapper.readValue(response.body(), Map.class);
                Map<String, Object> message = (Map<String, Object>) respMap.get("message");
                String content = (String) message.get("content");

                log.debug("Ollama response: length={}", content != null ? content.length() : 0);
                return content;
            } catch (Exception e) {
                log.error("Ollama API call failed: endpoint={}, model={}", endpoint, model, e);
                throw new RuntimeException("Ollama API call failed: " + e.getMessage(), e);
            }
        }, executor);
    }
}