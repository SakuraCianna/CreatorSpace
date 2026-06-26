package com.creatorspace.module.project.vo;

import com.creatorspace.module.tag.vo.TagVO;

import java.time.LocalDate;
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
        String reviewNote,
        List<ProjectImageVO> screenshots,
        List<ProjectMilestoneVO> milestones,
        List<ProjectLinkVO> resources,
        List<ProjectProcessNoteVO> processNotes
) {
    public record ProjectImageVO(String imageUrl, String caption, Integer sortOrder) {
    }

    public record ProjectMilestoneVO(String title, String description, LocalDate milestoneDate, Integer sortOrder) {
    }

    public record ProjectLinkVO(String kind, String label, String url, Integer sortOrder) {
    }

    public record ProjectProcessNoteVO(String phase, String title, String body, Integer sortOrder) {
    }
}
