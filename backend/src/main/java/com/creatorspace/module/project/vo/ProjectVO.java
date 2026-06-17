package com.creatorspace.module.project.vo;

import com.creatorspace.module.tag.vo.TagVO;

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
        List<TagVO> tags
) {
}
