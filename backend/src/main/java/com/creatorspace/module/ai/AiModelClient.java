package com.creatorspace.module.ai;

import com.creatorspace.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class AiModelClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String provider;
    private final String apiBaseUrl;
    private final String apiKey;
    private final String modelName;
    private final int requestTimeoutSeconds;
    private final int maxOutputTokens;

    public AiModelClient(
            ObjectMapper objectMapper,
            @Value("${app.ai.provider:zhipu}") String provider,
            @Value("${app.ai.zhipu-api-base-url:https://open.bigmodel.cn/api/paas/v4}") String apiBaseUrl,
            @Value("${app.ai.zhipu-api-key:}") String apiKey,
            @Value("${app.ai.zhipu-model:glm-4.6v-flash}") String modelName,
            @Value("${app.ai.request-timeout-seconds:60}") int requestTimeoutSeconds,
            @Value("${app.ai.max-output-tokens:4096}") int maxOutputTokens
    ) {
        this.objectMapper = objectMapper;
        this.provider = blankToDefault(provider, "zhipu");
        this.apiBaseUrl = trimTrailingSlash(blankToDefault(apiBaseUrl, "https://open.bigmodel.cn/api/paas/v4"));
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelName = blankToDefault(modelName, "glm-4.6v-flash");
        this.requestTimeoutSeconds = Math.max(5, requestTimeoutSeconds);
        this.maxOutputTokens = Math.max(256, maxOutputTokens);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, requestTimeoutSeconds)))
                .build();
    }

    public boolean supportsRemoteCall() {
        return !"local".equalsIgnoreCase(provider);
    }

    public String complete(List<ChatMessage> messages) {
        if (!supportsRemoteCall()) {
            throw BusinessException.badRequest("当前 AI_PROVIDER=local，不调用远程模型");
        }
        if (apiKey.isBlank()) {
            throw BusinessException.badRequest("AI 已启用，但未配置 ZHIPU_API_KEY。");
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", modelName);
            payload.put("stream", false);
            payload.put("temperature", 0.35);
            payload.put("max_tokens", maxOutputTokens);
            ArrayNode messageNodes = payload.putArray("messages");
            for (ChatMessage message : messages) {
                ObjectNode messageNode = messageNodes.addObject();
                messageNode.put("role", message.role().toLowerCase());
                messageNode.put("content", message.content());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw BusinessException.badRequest("AI 模型调用失败，HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw BusinessException.badRequest("AI 模型返回为空");
            }
            return content.trim();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw BusinessException.badRequest("AI 模型调用失败：" + exception.getMessage());
        }
    }

    public String completeStreaming(List<ChatMessage> messages, Consumer<String> onDelta) {
        if (!supportsRemoteCall()) {
            throw BusinessException.badRequest("当前 AI_PROVIDER=local，不调用远程模型");
        }
        if (apiKey.isBlank()) {
            throw BusinessException.badRequest("AI 已启用，但未配置 ZHIPU_API_KEY。");
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", modelName);
            payload.put("stream", true);
            payload.put("temperature", 0.35);
            payload.put("max_tokens", maxOutputTokens);
            ArrayNode messageNodes = payload.putArray("messages");
            for (ChatMessage message : messages) {
                ObjectNode messageNode = messageNodes.addObject();
                messageNode.put("role", message.role().toLowerCase());
                messageNode.put("content", message.content());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw BusinessException.badRequest("AI 模型调用失败：HTTP " + response.statusCode());
            }

            StringBuilder content = new StringBuilder();
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> readStreamDelta(line, content, onDelta));
            }
            if (content.isEmpty()) {
                throw BusinessException.badRequest("AI 模型返回为空");
            }
            return content.toString().trim();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw BusinessException.badRequest("AI 模型调用失败：" + exception.getMessage());
        }
    }

    private void readStreamDelta(String line, StringBuilder content, Consumer<String> onDelta) {
        String trimmed = line == null ? "" : line.trim();
        if (!trimmed.startsWith("data:")) {
            return;
        }
        String data = trimmed.substring(5).trim();
        if (data.isBlank() || "[DONE]".equals(data)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            String delta = root.path("choices").path(0).path("delta").path("content").asText("");
            if (delta.isBlank()) {
                delta = root.path("choices").path(0).path("message").path("content").asText("");
            }
            if (!delta.isBlank()) {
                content.append(delta);
                onDelta.accept(delta);
            }
        } catch (Exception ignored) {
            // Ignore malformed heartbeat lines from streaming providers.
        }
    }
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public record ChatMessage(String role, String content) {
    }
}
