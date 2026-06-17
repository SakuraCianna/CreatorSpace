package com.creatorspace.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 后台创建作品的请求参数。
 */
public record ProjectCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 220) String slug,
        String description,
        String coverUrl,
        @NotBlank @Size(max = 60) String projectType,
        List<String> techStack,
        String githubUrl,
        String demoUrl,
        String videoUrl,
        String contentMarkdown,
        List<Long> tagIds,
        Boolean recommended
) {
}
