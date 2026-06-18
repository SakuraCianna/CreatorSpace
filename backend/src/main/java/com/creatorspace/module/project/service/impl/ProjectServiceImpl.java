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

import java.net.URI;
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
        ensureSlugAvailable(slug, null);
        validateTagIds(request.tagIds());

        ProjectEntity project = new ProjectEntity();
        applyEditableFields(project, request, slug);
        project.setStatus(ContentConstants.PROJECT_VISIBLE);
        project.setSortOrder(0);
        project.setCreatedBy(operatorId);
        project.setUpdatedBy(operatorId);
        projectMapper.insertProject(project);
        replaceTags(project.getId(), request.tagIds());
        return toVO(project, true);
    }

    // 更新作品主体信息和标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO update(Long id, ProjectCreateRequest request, Long operatorId) {
        ProjectEntity project = requiredProject(id);
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, id);
        validateTagIds(request.tagIds());

        applyEditableFields(project, request, slug);
        project.setUpdatedBy(operatorId);
        projectMapper.updateProject(project);
        replaceTags(project.getId(), request.tagIds());
        return toVO(project, true);
    }

    // 删除作品及标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectEntity project = requiredProject(id);
        projectTagMapper.deleteByProjectId(project.getId());
        projectMapper.deleteById(project.getId());
    }

    // 设置作品展示状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO setStatus(Long id, String status, Long operatorId) {
        ProjectEntity project = requiredProject(id);
        project.setStatus(normalizeProjectStatus(status));
        project.setUpdatedBy(operatorId);
        projectMapper.updateProject(project);
        return toVO(project, true);
    }

    // 切换推荐状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO setRecommend(Long id, boolean enabled, Long operatorId) {
        ProjectEntity project = requiredProject(id);
        project.setRecommend(enabled);
        project.setUpdatedBy(operatorId);
        projectMapper.updateProject(project);
        return toVO(project, true);
    }

    // 管理员查询全部作品。
    @Override
    public PageResponse<ProjectVO> listAdmin(String keyword, String status, long page, long pageSize) {
        LambdaQueryWrapper<ProjectEntity> query = new LambdaQueryWrapper<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!normalizedKeyword.isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(ProjectEntity::getTitle, normalizedKeyword)
                    .or()
                    .like(ProjectEntity::getDescription, normalizedKeyword)
                    .or()
                    .like(ProjectEntity::getContentMarkdown, normalizedKeyword));
        }
        String normalizedStatus = normalizeOptionalStatus(status);
        if (!normalizedStatus.isEmpty()) {
            query.eq(ProjectEntity::getStatus, normalizedStatus);
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

    // 管理员按主键读取作品。
    @Override
    public ProjectVO getAdminById(Long id) {
        return toVO(requiredProject(id), true);
    }

    // 查询可公开展示的作品列表。
    @Override
    public PageResponse<ProjectVO> listPublic(String keyword, long page, long pageSize) {
        LambdaQueryWrapper<ProjectEntity> query = publicProjectQuery();
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

    // 按 URL 标识读取公开作品详情。
    @Override
    public ProjectVO getPublicBySlug(String slug) {
        ProjectEntity project = projectMapper.selectOne(publicProjectQuery()
                .eq(ProjectEntity::getSlug, normalizeSlug(slug)));
        if (project == null) {
            throw BusinessException.notFound("作品不存在或不可见");
        }
        return toVO(project, true);
    }

    // 构造公开作品基础查询条件，列表和详情共用可见性规则。
    private LambdaQueryWrapper<ProjectEntity> publicProjectQuery() {
        return new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getStatus, ContentConstants.PROJECT_VISIBLE);
    }

    // 确认内容标识未被占用。
    private void ensureSlugAvailable(String slug, Long ignoredId) {
        LambdaQueryWrapper<ProjectEntity> query = new LambdaQueryWrapper<ProjectEntity>().eq(ProjectEntity::getSlug, slug);
        if (ignoredId != null) {
            query.ne(ProjectEntity::getId, ignoredId);
        }
        Long count = projectMapper.selectCount(query);
        if (count > 0) {
            throw BusinessException.conflict("作品标识已存在");
        }
    }

    // 查询必须存在的作品。
    private ProjectEntity requiredProject(Long id) {
        ProjectEntity project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.notFound("作品不存在");
        }
        return project;
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

    // 将请求中可编辑字段写回实体。
    private void applyEditableFields(ProjectEntity project, ProjectCreateRequest request, String slug) {
        project.setTitle(request.title().trim());
        project.setSlug(slug);
        project.setDescription(blankToNull(request.description()));
        project.setCoverUrl(normalizeOptionalUrl(request.coverUrl(), "作品封面", true));
        project.setProjectType(request.projectType().trim());
        project.setTechStackJson(toJson(request.techStack()));
        project.setGithubUrl(normalizeOptionalUrl(request.githubUrl(), "GitHub 链接", false));
        project.setDemoUrl(normalizeOptionalUrl(request.demoUrl(), "演示链接", false));
        project.setVideoUrl(normalizeOptionalUrl(request.videoUrl(), "视频链接", true));
        project.setContentMarkdown(request.contentMarkdown() == null ? "" : request.contentMarkdown().trim());
        project.setRecommend(Boolean.TRUE.equals(request.recommended()));
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

    // 规范化作品状态。
    private String normalizeProjectStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ContentConstants.PROJECT_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest("作品状态不合法");
        }
        return normalized;
    }

    // 规范化可选作品状态筛选。
    private String normalizeOptionalStatus(String status) {
        String normalized = status == null || status.isBlank() ? "" : status.trim().toUpperCase();
        if (!normalized.isEmpty() && !ContentConstants.PROJECT_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest("作品状态不合法");
        }
        return normalized;
    }

    // 外部链接只允许 http/https；封面和视频允许引用站内上传资源。
    private String normalizeOptionalUrl(String value, String fieldName, boolean allowUploadedResource) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (allowUploadedResource && trimmed.startsWith("/uploads/")) {
            return trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return trimmed;
            }
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest(fieldName + "格式不合法");
        }
        throw BusinessException.badRequest(fieldName + "只允许 http 或 https 地址");
    }

    // 规范化 URL 标识。
    private String normalizeSlug(String slug) {
        String normalized = slug.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw BusinessException.badRequest("作品标识只允许小写字母、数字和短横线");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
