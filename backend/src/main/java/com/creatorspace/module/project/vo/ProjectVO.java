package com.creatorspace.module.project.vo;

import com.creatorspace.module.tag.vo.TagVO;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 返回给前端的作品视图对象。
 */
public record ProjectVO(
        Long id,
        String title,
        String slug,
        String description,
        String coverUrl,
        String projectType,
        List<String> techStack,
        String githubUrl,
        String demoUrl,
        String videoUrl,
        String contentMarkdown,
        String status,
        Boolean recommended,
        Long viewCount,
        Long likeCount,
        Long favoriteCount,
        Long commentCount,
        List<TagVO> tags,
        Long authorId,
        OffsetDateTime submittedAt,
        OffsetDateTime reviewedAt,
        String reviewNote
) {
}
