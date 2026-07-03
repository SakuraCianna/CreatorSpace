package com.creatorspace.module.ai;

import com.creatorspace.common.result.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/ai")
public class PublicAiController {

    private final AiAssistantService aiAssistantService;

    public PublicAiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/hot-topics")
    public List<String> generateHotTopics() {
        return aiAssistantService.generateHotTopics();
    }

    @PostMapping("/write")
    public ApiResponse<AiAssistantService.CreatorAiResponse> generateText(@Valid @RequestBody CreatorAiRequest request) {
        return ApiResponse.ok(aiAssistantService.generateCreatorText(new AiAssistantService.CreatorAiRequest(
                request.mode(),
                request.title(),
                request.prompt(),
                request.context(),
                request.selection()
        )));
    }

    public record CreatorAiRequest(
            @Size(max = 60) String mode,
            @Size(max = 120) String title,
            @Size(max = 4000) String prompt,
            @Size(max = 30000) String context,
            @Size(max = 8000) String selection
    ) {
    }
}
