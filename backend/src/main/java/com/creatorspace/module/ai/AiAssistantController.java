package com.creatorspace.module.ai;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/api/admin/ai/tasks")
    public ApiResponse<AiAssistantService.AiTaskVO> createTask(@Valid @RequestBody AiTaskRequest request) {
        return ApiResponse.ok(aiAssistantService.createTask(new AiAssistantService.AiTaskRequest(
                request.taskType(),
                request.targetType(),
                request.targetId(),
                request.prompt()
        )));
    }

    @GetMapping("/api/admin/ai/tasks/{id}")
    public ApiResponse<AiAssistantService.AiTaskVO> task(@PathVariable Long id) {
        return ApiResponse.ok(aiAssistantService.taskById(id));
    }

    @PostMapping("/api/admin/ai/tasks/{id}/messages")
    public ApiResponse<AiAssistantService.AiTaskVO> continueTask(@PathVariable Long id, @Valid @RequestBody AiContinueRequest request) {
        return ApiResponse.ok(aiAssistantService.continueTask(id, new AiAssistantService.AiContinueRequest(request.prompt())));
    }

    @PostMapping("/api/admin/ai/workflows")
    public ApiResponse<AiAssistantService.AiTaskVO> createWorkflow(@Valid @RequestBody AiWorkflowRequest request) {
        return ApiResponse.ok(aiAssistantService.createWorkflow(new AiAssistantService.AiWorkflowRequest(
                request.workflowType(),
                request.days()
        )));
    }

    @GetMapping("/api/admin/ai/suggestions")
    public ApiResponse<PageResponse<AiAssistantService.AiSuggestionVO>> suggestions(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(aiAssistantService.suggestions(status, page, pageSize));
    }

    @PutMapping("/api/admin/ai/suggestions/{id}/adopt")
    public ApiResponse<AiAssistantService.AiSuggestionVO> adopt(@PathVariable Long id) {
        return ApiResponse.ok(aiAssistantService.adopt(id));
    }

    @PutMapping("/api/admin/ai/suggestions/{id}/ignore")
    public ApiResponse<AiAssistantService.AiSuggestionVO> ignore(@PathVariable Long id) {
        return ApiResponse.ok(aiAssistantService.ignore(id));
    }

    public record AiTaskRequest(
            @NotBlank @Size(max = 60) String taskType,
            @Size(max = 60) String targetType,
            Long targetId,
            @NotBlank @Size(max = 4000) String prompt
    ) {
    }

    public record AiContinueRequest(
            @NotBlank @Size(max = 4000) String prompt
    ) {
    }

    public record AiWorkflowRequest(
            @NotBlank @Size(max = 60) String workflowType,
            @Min(1) @Max(90) Integer days
    ) {
    }
}