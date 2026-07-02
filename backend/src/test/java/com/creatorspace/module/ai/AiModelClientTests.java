package com.creatorspace.module.ai;

import com.creatorspace.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AiModelClientTests {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void localProviderRejectsRemoteCompletion() {
        AiModelClient client = new AiModelClient(new ObjectMapper(), " local ", "http://example.test///", "", " ", 1, 1);

        assertFalse(client.supportsRemoteCall());
        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.complete(List.of(new AiModelClient.ChatMessage("USER", "hello"))));
        assertTrue(exception.getMessage().contains("AI_PROVIDER=local"));
    }

    @Test
    void remoteProviderRequiresApiKey() {
        AiModelClient client = new AiModelClient(new ObjectMapper(), "zhipu", "http://example.test", " ", "glm", 5, 256);

        assertTrue(client.supportsRemoteCall());
        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.complete(List.of(new AiModelClient.ChatMessage("USER", "hello"))));
        assertTrue(exception.getMessage().contains("ZHIPU_API_KEY"));
    }

    @Test
    void completePostsChatMessagesAndReturnsTrimmedContent() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(200, """
                {"choices":[{"message":{"content":"  generated answer  "}}]}
                """, requestBody);
        AiModelClient client = remoteClient();

        String result = client.complete(List.of(
                new AiModelClient.ChatMessage("SYSTEM", "rules"),
                new AiModelClient.ChatMessage("USER", "prompt")
        ));

        assertEquals("generated answer", result);
        assertTrue(requestBody.get().contains("\"role\":\"system\""));
        assertTrue(requestBody.get().contains("\"content\":\"prompt\""));
    }

    @Test
    void completeRejectsHttpErrorsAndBlankModelContent() throws Exception {
        startServer(500, "boom", new AtomicReference<>());
        BusinessException httpError = assertThrows(BusinessException.class,
                () -> remoteClient().complete(List.of(new AiModelClient.ChatMessage("USER", "hello"))));
        assertTrue(httpError.getMessage().contains("HTTP 500"));
        server.stop(0);

        startServer(200, "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}", new AtomicReference<>());
        BusinessException blank = assertThrows(BusinessException.class,
                () -> remoteClient().complete(List.of(new AiModelClient.ChatMessage("USER", "hello"))));
        assertTrue(blank.getMessage().contains("返回为空"));
    }

    private AiModelClient remoteClient() {
        return new AiModelClient(
                new ObjectMapper(),
                "zhipu",
                "http://localhost:" + server.getAddress().getPort() + "///",
                "test-key",
                "glm-test",
                5,
                256
        );
    }

    private void startServer(int status, String responseBody, AtomicReference<String> requestBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }
}
