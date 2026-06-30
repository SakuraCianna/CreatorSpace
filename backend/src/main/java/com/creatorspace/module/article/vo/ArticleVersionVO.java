package com.creatorspace.module.article.vo;

import java.time.OffsetDateTime;

/**
 * 文章历史版本视图对象。
 */
public record ArticleVersionVO(
        Long id,
        Long articleId,
        Integer versionNo,
        String title,
        String summary,
        String contentMarkdown,
        Long createdBy,
        OffsetDateTime createdAt
) {
}
