package com.creatorspace.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.creatorspace.common.constant.ContentConstants;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.project.dto.ProjectCreateRequest;
import com.creatorspace.module.project.entity.ProjectEntity;
import com.creatorspace.module.project.mapper.ProjectMapper;
import com.creatorspace.module.project.mapper.ProjectTagMapper;
import com.creatorspace.module.project.service.ProjectService;
import com.creatorspace.module.project.vo.ProjectVO;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作品业务实现，负责作品创建、公开列表和技术栈 JSON 转换。
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectMapper projectMapper;
    private final ProjectTagMapper projectTagMapper;
    private final TagService tagService;

    // 通过构造器注入作品、标签和 JSON 序列化协作对象。
    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            ProjectTagMapper projectTagMapper,
            TagService tagService
    ) {
        this.projectMapper = projectMapper;
        this.projectTagMapper = projectTagMapper;
        this.tagService = tagService;
    }

    // 创建可见作品，并在同一个事务内写入标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO create(ProjectCreateRequest request, Long operatorId) {
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug);
        validateTagIds(request.tagIds());

        ProjectEntity project = new ProjectEntity();
        project.setTitle(request.title().trim());
        project.setSlug(slug);
        project.setDescription(request.description());
        project.setCoverUrl(request.coverUrl());
        project.setProjectType(request.projectType().trim());
        project.setTechStackJson(toJson(request.techStack()));
        project.setGithubUrl(request.githubUrl());
        project.setDemoUrl(request.demoUrl());
        project.setVideoUrl(request.videoUrl());
        project.setContentMarkdown(request.contentMarkdown() == null ? "" : request.contentMarkdown());
        project.setStatus(ContentConstants.PROJECT_VISIBLE);
        project.setRecommend(Boolean.TRUE.equals(request.recommended()));
        project.setSortOrder(0);
        project.setCreatedBy(operatorId);
        project.setUpdatedBy(operatorId);
        projectMapper.insertProject(project);
        replaceTags(project.getId(), request.tagIds());
        return toVO(project, true);
    }

    // 查询可公开展示的作品列表。
    @Override
    public PageResponse<ProjectVO> listPublic(String keyword, long page, long pageSize) {
        LambdaQueryWrapper<ProjectEntity> query = new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStatus, ContentConstants.PROJECT_VISIBLE);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!normalizedKeyword.isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(ProjectEntity::getTitle, normalizedKeyword)
                    .or()
                    .like(ProjectEntity::getDescription, normalizedKeyword));
        }
        query.orderByDesc(ProjectEntity::getRecommend)
                .orderByAsc(ProjectEntity::getSortOrder)
                .orderByDesc(ProjectEntity::getId);
        Page<ProjectEntity> result = projectMapper.selectPage(new Page<>(page, pageSize), query);
        List<ProjectVO> records = result.getRecords()
                .stream()
                .map(project -> toVO(project, false))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    // 确认内容标识未被占用。
    private void ensureSlugAvailable(String slug) {
        Long count = projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>().eq(ProjectEntity::getSlug, slug));
        if (count > 0) {
            throw BusinessException.conflict("作品标识已存在");
        }
    }

    // 校验标签集合存在，避免作品创建依赖数据库外键异常表达业务错误。
    private void validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        if (tagIds.stream().anyMatch(Objects::isNull)) {
            throw BusinessException.badRequest("标签不存在");
        }
        Set<Long> requestedIds = tagIds.stream().collect(Collectors.toSet());
        Set<Long> existingIds = tagService.listByIds(List.copyOf(requestedIds))
                .stream()
                .map(TagVO::id)
                .collect(Collectors.toSet());
        if (!existingIds.containsAll(requestedIds)) {
            throw BusinessException.badRequest("标签不存在");
        }
    }

    // 替换内容与标签的绑定关系。
    private void replaceTags(Long projectId, List<Long> tagIds) {
        projectTagMapper.deleteByProjectId(projectId);
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            projectTagMapper.insertIgnore(projectId, tagId);
        }
    }

    // 转换为前端可用的视图对象。
    private ProjectVO toVO(ProjectEntity entity, boolean includeContent) {
        List<Long> tagIds = projectTagMapper.selectTagIdsByProjectId(entity.getId());
        List<TagVO> tags = tagIds.isEmpty() ? Collections.emptyList() : tagService.listByIds(tagIds);
        return new ProjectVO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getCoverUrl(),
                entity.getProjectType(),
                fromJson(entity.getTechStackJson()),
                entity.getGithubUrl(),
                entity.getDemoUrl(),
                entity.getVideoUrl(),
                includeContent ? entity.getContentMarkdown() : null,
                entity.getStatus(),
                entity.getRecommend(),
                tags
        );
    }

    // 将列表数据序列化为 JSON。
    private String toJson(List<String> techStack) {
        try {
            return objectMapper.writeValueAsString(techStack == null ? Collections.emptyList() : techStack);
        } catch (Exception exception) {
            throw BusinessException.badRequest("技术栈格式不合法");
        }
    }

    // 将 JSON 反序列化为列表数据。
    private List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value == null ? "[]" : value, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    // 规范化 URL 标识。
    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }
}
