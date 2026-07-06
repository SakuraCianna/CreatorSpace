package com.creatorspace.module.ai;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.audit.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistantServiceTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final AiAssistantService service = new AiAssistantService(
            jdbcTemplate,
            aiModelClient,
            operationLogService,
            true,
            "zhipu",
            "glm-test"
    );

    @Test
    void creatorStreamingFallsBackToLocalTextInChunks() {
        when(aiModelClient.completeStreaming(anyList(), any()))
                .thenThrow(BusinessException.badRequest("offline"));
        List<String> deltas = new ArrayList<>();

        AiAssistantService.CreatorAiResponse response = service.generateCreatorTextStreaming(
                new AiAssistantService.CreatorAiRequest("SUMMARY", "个性化博客", null, null, null),
                deltas::add
        );

        assertEquals("SUMMARY", response.mode());
        assertFalse(response.text().isBlank());
        assertEquals(response.text(), String.join("", deltas));
        verify(aiModelClient).completeStreaming(anyList(), any());
    }

    @Test
    void creatorStreamingDoesNotFallbackAfterRemoteDeltaStarted() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<String> onDelta = invocation.getArgument(1, Consumer.class);
            onDelta.accept("远程片段");
            throw BusinessException.badRequest("remote interrupted");
        }).when(aiModelClient).completeStreaming(anyList(), any());
        List<String> deltas = new ArrayList<>();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.generateCreatorTextStreaming(
                        new AiAssistantService.CreatorAiRequest("CUSTOM", null, "继续解释", null, null),
                        deltas::add
                )
        );

        assertEquals("远程片段", String.join("", deltas));
        assertEquals("remote interrupted", exception.getMessage());
    }
}
