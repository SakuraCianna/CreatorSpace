package com.creatorspace.module.ai;

import com.creatorspace.common.result.PageResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiAssistantControllerTests {

    private final AiAssistantService aiAssistantService = mock(AiAssistantService.class);
    private final AiAssistantController controller = new AiAssistantController(aiAssistantService);

    @Test
    void createTaskDelegatesToService() {
        AiAssistantService.AiTaskVO task = task(1L, "SUMMARY");
        when(aiAssistantService.createTask(any(AiAssistantService.AiTaskRequest.class))).thenReturn(task);

        var response = controller.createTask(new AiAssistantController.AiTaskRequest(
                "SUMMARY", "ARTICLE", 7L, "帮我生成摘要"));

        assertTrue(response.success());
        assertEquals(task, response.data());
        verify(aiAssistantService).createTask(new AiAssistantService.AiTaskRequest("SUMMARY", "ARTICLE", 7L, "帮我生成摘要"));
    }

    @Test
    void continueTaskAndWorkflowDelegateToService() {
        when(aiAssistantService.continueTask(eq(3L), any(AiAssistantService.AiContinueRequest.class))).thenReturn(task(3L, "SUMMARY"));
        when(aiAssistantService.createWorkflow(any(AiAssistantService.AiWorkflowRequest.class))).thenReturn(task(4L, "OPERATION_REPORT"));

        assertEquals(3L, controller.continueTask(3L, new AiAssistantController.AiContinueRequest("继续")).data().id());
        assertEquals(4L, controller.createWorkflow(new AiAssistantController.AiWorkflowRequest("OPERATION_REPORT", 7)).data().id());

        verify(aiAssistantService).continueTask(3L, new AiAssistantService.AiContinueRequest("继续"));
        verify(aiAssistantService).createWorkflow(new AiAssistantService.AiWorkflowRequest("OPERATION_REPORT", 7));
    }

    @Test
    void suggestionsAdoptIgnoreAndTaskByIdReturnWrappedData() {
        PageResponse<AiAssistantService.AiSuggestionVO> page = new PageResponse<>(List.of(suggestion(1L, "PENDING")), 1, 20, 1);
        when(aiAssistantService.taskById(9L)).thenReturn(task(9L, "TAGS"));
        when(aiAssistantService.suggestions("PENDING", 1, 20)).thenReturn(page);
        when(aiAssistantService.adopt(1L)).thenReturn(suggestion(1L, "ADOPTED"));
        when(aiAssistantService.ignore(2L)).thenReturn(suggestion(2L, "REJECTED"));

        assertEquals(9L, controller.task(9L).data().id());
        assertEquals(1, controller.suggestions("PENDING", 1, 20).data().records().size());
        assertEquals("ADOPTED", controller.adopt(1L).data().status());
        assertEquals("REJECTED", controller.ignore(2L).data().status());
    }

    private AiAssistantService.AiTaskVO task(Long id, String taskType) {
        return new AiAssistantService.AiTaskVO(
                id,
                taskType,
                "ARTICLE",
                7L,
                "prompt",
                "SUCCEEDED",
                "local",
                "local-rule-assistant",
                1L,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of(),
                List.of(),
                null
        );
    }

    private AiAssistantService.AiSuggestionVO suggestion(Long id, String status) {
        return new AiAssistantService.AiSuggestionVO(
                id,
                1L,
                "ARTICLE",
                7L,
                "SUMMARY",
                "content",
                status,
                null,
                null,
                OffsetDateTime.now()
        );
    }
}
