package com.creatorspace.module.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
    public String generateText(@RequestBody Map<String, String> request) {
        return aiAssistantService.generateText(request.get("prompt"), request.get("context"));
    }
}
