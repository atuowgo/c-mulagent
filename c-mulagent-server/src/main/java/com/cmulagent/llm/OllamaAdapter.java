package com.cmulagent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OllamaAdapter implements LLMClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaAdapter.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient httpClient;
    private final String endpoint;
    private final String model;

    public OllamaAdapter(String endpoint, String model) {
        this.endpoint = endpoint;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<Message> messages) {
        log.debug("Ollama request: endpoint={}, model={}, messageCount={}", endpoint, model, messages.size());

        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("stream", false);

            ArrayNode msgArray = mapper.createArrayNode();
            ObjectNode sysMsg = mapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            msgArray.add(sysMsg);

            for (LLMClient.Message msg : messages) {
                ObjectNode m = mapper.createObjectNode();
                m.put("role", msg.role());
                m.put("content", msg.content());
                msgArray.add(m);
            }
            body.set("messages", msgArray);

            Request request = new Request.Builder()
                    .url(endpoint + "/api/chat")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(mapper.writeValueAsString(body), JSON_MEDIA_TYPE))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "no body";
                            log.error("Ollama HTTP {}: {}", response.code(), errorBody);
                            future.completeExceptionally(
                                    new RuntimeException("Ollama API returned HTTP " + response.code() + ": " + errorBody));
                            return;
                        }

                        String responseBody = response.body() != null ? response.body().string() : "";
                        JsonNode root = mapper.readTree(responseBody);
                        String content = root.at("/message/content").asText();
                        log.debug("Ollama response: length={}", content.length());
                        future.complete(content);
                    } catch (Exception e) {
                        log.error("Ollama response parsing failed", e);
                        future.completeExceptionally(new RuntimeException("Ollama response parsing failed: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("Ollama request failed", e);
                    future.completeExceptionally(new RuntimeException("Ollama request failed: " + e.getMessage(), e));
                }
            });
        } catch (Exception e) {
            log.error("Ollama request building failed", e);
            future.completeExceptionally(e);
        }

        return future;
    }
}