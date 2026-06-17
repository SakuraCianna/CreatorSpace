package com.creatorspace.module.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 后台创建文章的请求参数。
 */
public record ArticleCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 220) String slug,
        String summary,
        @NotBlank String contentMarkdown,
        String coverUrl,
        Long categoryId,
        List<Long> tagIds,
        @NotBlank String privacyType
) {
}
