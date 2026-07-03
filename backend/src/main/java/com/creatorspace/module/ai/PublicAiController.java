package com.creatorspace.module.ai;

import com.creatorspace.common.result.ApiResponse;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequestMapping("/api/ai")
public class PublicAiController {

    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final AiAssistantService aiAssistantService;
    private final ExecutorService streamExecutor = new ThreadPoolExecutor(
            2,
            8,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public PublicAiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PreDestroy
    void shutdownStreamExecutor() {
        streamExecutor.shutdownNow();
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

    @PostMapping("/write/stream")
    public SseEmitter generateTextStream(@Valid @RequestBody CreatorAiRequest request) {
        SseEmitter emitter = createStreamEmitter();
        streamExecutor.execute(() -> {
            try {
                AiAssistantService.CreatorAiResponse response = aiAssistantService.generateCreatorTextStreaming(
                        new AiAssistantService.CreatorAiRequest(
                                request.mode(),
                                request.title(),
                                request.prompt(),
                                request.context(),
                                request.selection()
                        ),
                        delta -> sendEvent(emitter, "delta", delta)
                );
                sendEvent(emitter, "done", response);
                emitter.complete();
            } catch (Exception exception) {
                completeWithError(emitter, exception);
            }
        });
        return emitter;
    }

    private SseEmitter createStreamEmitter() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onTimeout(() -> completeWithError(emitter, new IllegalStateException("AI 流式生成超时，请稍后重试")));
        emitter.onError(emitter::completeWithError);
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void completeWithError(SseEmitter emitter, Exception exception) {
        try {
            sendEvent(emitter, "error", exception.getMessage() == null ? "AI 流式生成失败" : exception.getMessage());
            emitter.complete();
        } catch (Exception ignored) {
            emitter.completeWithError(exception);
        }
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
