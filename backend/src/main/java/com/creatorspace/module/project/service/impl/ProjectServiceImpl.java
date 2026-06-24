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
import com.creatorspace.module.statistics.VisitLogService;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
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
    private final JdbcTemplate jdbcTemplate;
    private final VisitLogService visitLogService;

    // 通过构造器注入作品、标签和 JSON 序列化协作对象。
    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            ProjectTagMapper projectTagMapper,
            TagService tagService,
            JdbcTemplate jdbcTemplate,
            VisitLogService visitLogService
    ) {
        this.projectMapper = projectMapper;
        this.projectTagMapper = projectTagMapper;
        this.tagService = tagService;
        this.jdbcTemplate = jdbcTemplate;
        this.visitLogService = visitLogService;
    }

    // 创建作品草稿，并在同一个事务内写入标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO create(ProjectCreateRequest request, Long operatorId) {
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, null);
        validateTagIds(request.tagIds());

        ProjectEntity project = new ProjectEntity();
        applyEditableFields(project, request, slug);
        project.setStatus(ContentConstants.PROJECT_DRAFT);
        project.setSortOrder(0);
        project.setSubmittedAt(null);
        project.setReviewedBy(null);
        project.setReviewedAt(null);
        project.setReviewNote(null);
        project.setCreatedBy(operatorId);
        project.setUpdatedBy(operatorId);
        projectMapper.insertProject(project);
        replaceTags(project.getId(), request.tagIds());
        return toVO(project, true);
    }

    // 当前创作者创建作品草稿，推荐位只能由管理员运营。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO createMine(ProjectCreateRequest request, Long userId) {
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, null);
        validateTagIds(request.tagIds());

        ProjectEntity project = new ProjectEntity();
        applyEditableFields(project, request, slug);
        project.setRecommend(false);
        project.setStatus(ContentConstants.PROJECT_DRAFT);
        project.setSortOrder(0);
        project.setSubmittedAt(null);
        project.setReviewedBy(null);
        project.setReviewedAt(null);
        project.setReviewNote(null);
        project.setCreatedBy(userId);
        project.setUpdatedBy(userId);
        projectMapper.insertProject(project);
        replaceTags(project.getId(), request.tagIds());
        return toVO(project, true);
    }

    // 查询当前创作者自己的作品队列。
    @Override
    public PageResponse<ProjectVO> listMine(Long userId, String keyword, String status, long page, long pageSize) {
        LambdaQueryWrapper<ProjectEntity> query = new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getCreatedBy, userId);
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
        query.orderByDesc(ProjectEntity::getId);
        Page<ProjectEntity> result = projectMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResponse<>(
                result.getRecords().stream().map(project -> toVO(project, false)).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal()
        );
    }

    // 当前创作者读取自己的作品。
    @Override
    public ProjectVO getMineById(Long id, Long userId) {
        return toVO(requiredOwnedProject(id, userId), true);
    }

    // 当前创作者更新自己的作品草稿或驳回作品。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO updateMine(Long id, ProjectCreateRequest request, Long userId) {
        ProjectEntity project = requiredOwnedProject(id, userId);
        ensureCreatorEditable(project);
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, id);
        validateTagIds(request.tagIds());

        applyEditableFields(project, request, slug);
        project.setRecommend(false);
        project.setStatus(ContentConstants.PROJECT_DRAFT);
        project.setSubmittedAt(null);
        project.setReviewedBy(null);
        project.setReviewedAt(null);
        project.setReviewNote(null);
        project.setUpdatedBy(userId);
        projectMapper.updateProject(project);
        replaceTags(project.getId(), request.tagIds());
        return toVO(project, true);
    }

    // 当前创作者提交作品进入管理员审核队列。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO submitForReview(Long id, Long userId) {
        ProjectEntity project = requiredOwnedProject(id, userId);
        ensureCreatorEditable(project);
        OffsetDateTime submittedAt = OffsetDateTime.now();
        projectMapper.updateReviewState(
                id,
                ContentConstants.PROJECT_PENDING_REVIEW,
                submittedAt,
                null,
                null,
                null,
                userId
        );
        project.setStatus(ContentConstants.PROJECT_PENDING_REVIEW);
        project.setSubmittedAt(submittedAt);
        project.setReviewedBy(null);
        project.setReviewedAt(null);
        project.setReviewNote(null);
        return toVO(project, true);
    }

    // 当前创作者只能删除未公开作品。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMine(Long id, Long userId) {
        ProjectEntity project = requiredOwnedProject(id, userId);
        if (Set.of(ContentConstants.PROJECT_VISIBLE, ContentConstants.PROJECT_HIDDEN, ContentConstants.PROJECT_ARCHIVED)
                .contains(project.getStatus())) {
            throw BusinessException.badRequest("已展示或归档作品不能由创作者直接删除");
        }
        projectTagMapper.deleteByProjectId(project.getId());
        projectMapper.deleteById(project.getId());
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

    // 管理员审核通过作品，让它进入公开作品展厅。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO approve(Long id, Long operatorId) {
        ProjectEntity project = requiredProject(id);
        OffsetDateTime reviewedAt = OffsetDateTime.now();
        OffsetDateTime submittedAt = project.getSubmittedAt() == null ? reviewedAt : project.getSubmittedAt();
        projectMapper.updateReviewState(
                id,
                ContentConstants.PROJECT_VISIBLE,
                submittedAt,
                operatorId,
                reviewedAt,
                null,
                operatorId
        );
        project.setStatus(ContentConstants.PROJECT_VISIBLE);
        project.setSubmittedAt(submittedAt);
        project.setReviewedBy(operatorId);
        project.setReviewedAt(reviewedAt);
        project.setReviewNote(null);
        return toVO(project, true);
    }

    // 管理员驳回作品，保留可执行的修改意见。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO reject(Long id, String reviewNote, Long operatorId) {
        ProjectEntity project = requiredProject(id);
        OffsetDateTime reviewedAt = OffsetDateTime.now();
        OffsetDateTime submittedAt = project.getSubmittedAt() == null ? reviewedAt : project.getSubmittedAt();
        String note = blankToNull(reviewNote);
        projectMapper.updateReviewState(
                id,
                ContentConstants.PROJECT_REJECTED,
                submittedAt,
                operatorId,
                reviewedAt,
                note == null ? "作品暂未通过审核，请修改后重新提交。" : note,
                operatorId
        );
        project.setStatus(ContentConstants.PROJECT_REJECTED);
        project.setSubmittedAt(submittedAt);
        project.setReviewedBy(operatorId);
        project.setReviewedAt(reviewedAt);
        project.setReviewNote(note == null ? "作品暂未通过审核，请修改后重新提交。" : note);
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
    public ProjectVO getPublicBySlug(String slug, HttpServletRequest request) {
        ProjectEntity project = projectMapper.selectOne(publicProjectQuery()
                .eq(ProjectEntity::getSlug, normalizeSlug(slug)));
        if (project == null) {
            throw BusinessException.notFound("作品不存在或不可见");
        }
        if (visitLogService.recordContentVisit("PROJECT", project.getId(), request)) {
            incrementViewCount(project.getId());
        }
        return toVO(project, true);
    }

    // 自增作品阅读量，同步更新 content_statistics 表。
    private void incrementViewCount(Long projectId) {
        jdbcTemplate.update("""
                insert into content_statistics (target_type, target_id, view_count, last_viewed_at, updated_at)
                values ('PROJECT', ?, 1, now(), now())
                on conflict (target_type, target_id) do update
                set view_count = content_statistics.view_count + 1,
                    last_viewed_at = now(),
                    updated_at = now()
                """,
                projectId
        );
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
        ProjectStats stats = projectStats(entity.getId());
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
                stats.viewCount(),
                stats.likeCount(),
                stats.favoriteCount(),
                stats.commentCount(),
                tags,
                entity.getCreatedBy(),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getReviewNote()
        );
    }

    private ProjectStats projectStats(Long projectId) {
        return jdbcTemplate.query("""
                        select view_count, like_count, favorite_count, comment_count
                        from content_statistics
                        where target_type = 'PROJECT'
                          and target_id = ?
                        """,
                (rs, rowNum) -> new ProjectStats(
                        rs.getLong("view_count"),
                        rs.getLong("like_count"),
                        rs.getLong("favorite_count"),
                        rs.getLong("comment_count")
                ),
                projectId
        ).stream().findFirst().orElse(ProjectStats.ZERO);
    }

    private record ProjectStats(Long viewCount, Long likeCount, Long favoriteCount, Long commentCount) {
        static final ProjectStats ZERO = new ProjectStats(0L, 0L, 0L, 0L);
    }

    // 查询必须属于当前创作者的作品。
    private ProjectEntity requiredOwnedProject(Long id, Long userId) {
        ProjectEntity project = requiredProject(id);
        if (!Objects.equals(project.getCreatedBy(), userId)) {
            throw BusinessException.forbidden("只能操作自己的作品");
        }
        return project;
    }

    // 创作者只允许编辑未公开传播的作品。
    private void ensureCreatorEditable(ProjectEntity project) {
        if (!Set.of(ContentConstants.PROJECT_DRAFT, ContentConstants.PROJECT_REJECTED).contains(project.getStatus())) {
            throw BusinessException.badRequest("当前状态不能编辑，请等待审核或联系管理员");
        }
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
