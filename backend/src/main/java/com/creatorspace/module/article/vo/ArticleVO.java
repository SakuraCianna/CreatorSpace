package com.creatorspace.module.article.vo;

import com.creatorspace.module.category.vo.CategoryVO;
import com.creatorspace.module.tag.vo.TagVO;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 前台和后台共用的文章视图对象。
 */
public record ArticleVO(
        Long id,
        String title,
        String slug,
        String summary,
        String contentMarkdown,
        String coverUrl,
        String status,
        String privacyType,
        Boolean top,
        Boolean recommended,
        CategoryVO category,
        List<TagVO> tags,
        OffsetDateTime publishTime,
        Long authorId,
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt,
        String reviewNote
) {
}
