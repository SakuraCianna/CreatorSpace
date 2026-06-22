package com.creatorspace.module.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.creatorspace.common.constant.ContentConstants;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.article.dto.ArticleCreateRequest;
import com.creatorspace.module.article.entity.ArticleEntity;
import com.creatorspace.module.article.mapper.ArticleMapper;
import com.creatorspace.module.article.mapper.ArticleTagMapper;
import com.creatorspace.module.article.service.ArticleService;
import com.creatorspace.module.article.vo.ArticleVO;
import com.creatorspace.module.category.service.CategoryService;
import com.creatorspace.module.category.vo.CategoryVO;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
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
 * 文章业务实现，负责草稿、发布和公开读取规则。
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final JdbcTemplate jdbcTemplate;

    // 通过构造器注入文章、分类和标签协作服务。
    public ArticleServiceImpl(
            ArticleMapper articleMapper,
            ArticleTagMapper articleTagMapper,
            CategoryService categoryService,
            TagService tagService,
            JdbcTemplate jdbcTemplate
    ) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // 创建草稿文章，并在同一个事务内写入标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO create(ArticleCreateRequest request, Long operatorId) {
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, null);
        String privacyType = normalizePrivacyType(request.privacyType());
        validateCategoryForArticle(request.categoryId());
        validateTagIds(request.tagIds());

        ArticleEntity article = new ArticleEntity();
        applyEditableFields(article, request, slug, privacyType);
        article.setStatus(ContentConstants.STATUS_DRAFT);
        article.setTop(false);
        article.setRecommend(false);
        article.setSubmittedAt(null);
        article.setReviewedBy(null);
        article.setReviewedAt(null);
        article.setReviewNote(null);
        article.setCreatedBy(operatorId);
        article.setUpdatedBy(operatorId);
        articleMapper.insert(article);
        replaceTags(article.getId(), request.tagIds());
        return toVO(article, true);
    }

    // 查询当前创作者自己的文章队列。
    @Override
    public PageResponse<ArticleVO> listMine(Long userId, String keyword, String status, long page, long pageSize) {
        LambdaQueryWrapper<ArticleEntity> query = new LambdaQueryWrapper<ArticleEntity>()
                .eq(ArticleEntity::getCreatedBy, userId);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!normalizedKeyword.isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(ArticleEntity::getTitle, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getSummary, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getContentMarkdown, normalizedKeyword));
        }
        String normalizedStatus = normalizeOptionalStatus(status);
        if (!normalizedStatus.isEmpty()) {
            query.eq(ArticleEntity::getStatus, normalizedStatus);
        }
        query.orderByDesc(ArticleEntity::getId);
        Page<ArticleEntity> result = articleMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResponse<>(
                result.getRecords().stream().map(article -> toVO(article, false)).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal()
        );
    }

    // 当前创作者读取自己的文章。
    @Override
    public ArticleVO getMineById(Long id, Long userId) {
        ArticleEntity article = requiredOwnedArticle(id, userId);
        return toVO(article, true);
    }

    // 当前创作者更新自己的草稿或驳回文章。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO updateMine(Long id, ArticleCreateRequest request, Long userId) {
        ArticleEntity article = requiredOwnedArticle(id, userId);
        ensureCreatorEditable(article);
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, id);
        String privacyType = normalizePrivacyType(request.privacyType());
        validateCategoryForArticle(request.categoryId());
        validateTagIds(request.tagIds());

        applyEditableFields(article, request, slug, privacyType);
        article.setStatus(ContentConstants.STATUS_DRAFT);
        article.setSubmittedAt(null);
        article.setReviewedBy(null);
        article.setReviewedAt(null);
        article.setReviewNote(null);
        article.setPublishTime(null);
        article.setUpdatedBy(userId);
        articleMapper.updateEditableArticle(article);
        replaceTags(article.getId(), request.tagIds());
        return toVO(article, true);
    }

    // 当前创作者提交文章进入管理员审核队列。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO submitForReview(Long id, Long userId) {
        ArticleEntity article = requiredOwnedArticle(id, userId);
        ensureCreatorEditable(article);
        OffsetDateTime submittedAt = OffsetDateTime.now();
        articleMapper.updateReviewState(
                id,
                ContentConstants.STATUS_PENDING_REVIEW,
                submittedAt,
                null,
                null,
                null,
                null,
                userId
        );
        article.setStatus(ContentConstants.STATUS_PENDING_REVIEW);
        article.setSubmittedAt(submittedAt);
        article.setReviewedBy(null);
        article.setReviewedAt(null);
        article.setReviewNote(null);
        article.setPublishTime(null);
        return toVO(article, true);
    }

    // 当前创作者只能删除未公开内容，避免误删已经进入公开传播的内容。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMine(Long id, Long userId) {
        ArticleEntity article = requiredOwnedArticle(id, userId);
        if (Set.of(ContentConstants.STATUS_PUBLISHED, ContentConstants.STATUS_PRIVATE, ContentConstants.STATUS_ARCHIVED)
                .contains(article.getStatus())) {
            throw BusinessException.badRequest("已公开或归档文章不能由创作者直接删除");
        }
        articleTagMapper.deleteByArticleId(article.getId());
        articleMapper.deleteById(article.getId());
    }

    // 更新文章主体内容，已发布文章会根据可见性同步公开/私密状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO update(Long id, ArticleCreateRequest request, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug, id);
        String privacyType = normalizePrivacyType(request.privacyType());
        validateCategoryForArticle(request.categoryId());
        validateTagIds(request.tagIds());

        applyEditableFields(article, request, slug, privacyType);
        if (Set.of(ContentConstants.STATUS_PUBLISHED, ContentConstants.STATUS_PRIVATE).contains(article.getStatus())) {
            article.setStatus(publishedStatusFor(privacyType));
        }
        article.setUpdatedBy(operatorId);
        articleMapper.updateEditableArticle(article);
        replaceTags(article.getId(), request.tagIds());
        return toVO(article, true);
    }

    // 发布文章时根据可见性计算最终状态，私密内容不会进入公开列表。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO publish(Long id, Long operatorId) {
        return approve(id, operatorId);
    }

    // 撤回文章到草稿状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO unpublish(Long id, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        articleMapper.updateReviewState(id, ContentConstants.STATUS_DRAFT, null, null, null, null, null, operatorId);
        article.setStatus(ContentConstants.STATUS_DRAFT);
        article.setSubmittedAt(null);
        article.setReviewedBy(null);
        article.setReviewedAt(null);
        article.setReviewNote(null);
        article.setPublishTime(null);
        return toVO(article, true);
    }

    // 管理员审核通过文章，根据隐私可见性决定是否进入公开列表。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO approve(Long id, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        String status = publishedStatusFor(article.getPrivacyType());
        OffsetDateTime reviewedAt = OffsetDateTime.now();
        OffsetDateTime publishTime = reviewedAt;
        OffsetDateTime submittedAt = article.getSubmittedAt() == null ? reviewedAt : article.getSubmittedAt();
        articleMapper.updateReviewState(id, status, submittedAt, operatorId, reviewedAt, null, publishTime, operatorId);
        article.setStatus(status);
        article.setSubmittedAt(submittedAt);
        article.setReviewedBy(operatorId);
        article.setReviewedAt(reviewedAt);
        article.setReviewNote(null);
        article.setPublishTime(publishTime);
        return toVO(article, true);
    }

    // 管理员驳回文章，保留审核意见供创作者修改后重新提交。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO reject(Long id, String reviewNote, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        OffsetDateTime reviewedAt = OffsetDateTime.now();
        OffsetDateTime submittedAt = article.getSubmittedAt() == null ? reviewedAt : article.getSubmittedAt();
        String note = blankToNull(reviewNote);
        articleMapper.updateReviewState(
                id,
                ContentConstants.STATUS_REJECTED,
                submittedAt,
                operatorId,
                reviewedAt,
                note == null ? "内容暂未通过审核，请修改后重新提交。" : note,
                null,
                operatorId
        );
        article.setStatus(ContentConstants.STATUS_REJECTED);
        article.setSubmittedAt(submittedAt);
        article.setReviewedBy(operatorId);
        article.setReviewedAt(reviewedAt);
        article.setReviewNote(note == null ? "内容暂未通过审核，请修改后重新提交。" : note);
        article.setPublishTime(null);
        return toVO(article, true);
    }

    // 删除文章及其标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ArticleEntity article = requiredArticle(id);
        articleTagMapper.deleteByArticleId(article.getId());
        articleMapper.deleteById(article.getId());
    }

    // 切换置顶状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO setTop(Long id, boolean enabled, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        article.setTop(enabled);
        article.setUpdatedBy(operatorId);
        articleMapper.updateById(article);
        return toVO(article, true);
    }

    // 切换推荐状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO setRecommend(Long id, boolean enabled, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        article.setRecommend(enabled);
        article.setUpdatedBy(operatorId);
        articleMapper.updateById(article);
        return toVO(article, true);
    }

    // 管理员查询全部文章，支持关键字和状态筛选。
    @Override
    public PageResponse<ArticleVO> listAdmin(String keyword, String status, long page, long pageSize) {
        LambdaQueryWrapper<ArticleEntity> query = new LambdaQueryWrapper<>();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!normalizedKeyword.isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(ArticleEntity::getTitle, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getSummary, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getContentMarkdown, normalizedKeyword));
        }
        String normalizedStatus = normalizeOptionalStatus(status);
        if (!normalizedStatus.isEmpty()) {
            query.eq(ArticleEntity::getStatus, normalizedStatus);
        }
        query.orderByDesc(ArticleEntity::getTop)
                .orderByDesc(ArticleEntity::getPublishTime)
                .orderByDesc(ArticleEntity::getId);

        Page<ArticleEntity> result = articleMapper.selectPage(new Page<>(page, pageSize), query);
        List<ArticleVO> records = result.getRecords()
                .stream()
                .map(article -> toVO(article, false))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    // 管理员按主键读取文章，包含正文。
    @Override
    public ArticleVO getAdminById(Long id) {
        return toVO(requiredArticle(id), true);
    }

    // 查询公开文章列表，支持关键字和标签筛选。
    @Override
    public PageResponse<ArticleVO> listPublic(String keyword, Long tagId, long page, long pageSize) {
        if (tagId != null) {
            return listPublicByTag(tagId, keyword, page, pageSize);
        }
        LambdaQueryWrapper<ArticleEntity> query = publicArticleQuery();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!normalizedKeyword.isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(ArticleEntity::getTitle, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getSummary, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getContentMarkdown, normalizedKeyword));
        }
        query.orderByDesc(ArticleEntity::getTop)
                .orderByDesc(ArticleEntity::getPublishTime)
                .orderByDesc(ArticleEntity::getId);

        Page<ArticleEntity> result = articleMapper.selectPage(new Page<>(page, pageSize), query);
        List<ArticleVO> records = result.getRecords()
                .stream()
                .map(article -> toVO(article, false))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    // 按标签筛选公开文章，通过 article_tags 关联表过滤。
    private PageResponse<ArticleVO> listPublicByTag(Long tagId, String keyword, long page, long pageSize) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Long> articleIds = articleTagMapper.selectArticleIdsByTagId(tagId);
        if (articleIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, pageSize, 0);
        }
        LambdaQueryWrapper<ArticleEntity> query = publicArticleQuery()
                .in(ArticleEntity::getId, articleIds);
        if (!normalizedKeyword.isEmpty()) {
            query.and(wrapper -> wrapper
                    .like(ArticleEntity::getTitle, normalizedKeyword)
                    .or()
                    .like(ArticleEntity::getSummary, normalizedKeyword));
        }
        query.orderByDesc(ArticleEntity::getTop)
                .orderByDesc(ArticleEntity::getPublishTime)
                .orderByDesc(ArticleEntity::getId);
        Page<ArticleEntity> result = articleMapper.selectPage(new Page<>(page, pageSize), query);
        List<ArticleVO> records = result.getRecords()
                .stream()
                .map(article -> toVO(article, false))
                .toList();
        return new PageResponse<>(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    // 按 URL 标识读取公开文章详情，并自增阅读量。
    @Override
    public ArticleVO getPublicBySlug(String slug) {
        ArticleEntity article = articleMapper.selectOne(publicArticleQuery()
                .eq(ArticleEntity::getSlug, normalizeSlug(slug)));
        if (article == null) {
            throw BusinessException.notFound("文章不存在或不可见");
        }
        incrementViewCount(article.getId());
        return toVO(article, true);
    }

    // 自增文章阅读量，同步更新 articles 表和 content_statistics 表。
    private void incrementViewCount(Long articleId) {
        jdbcTemplate.update(
                "update articles set view_count = view_count + 1, updated_at = now() where id = ?",
                articleId
        );
        jdbcTemplate.update("""
                insert into content_statistics (target_type, target_id, view_count, last_viewed_at, updated_at)
                values ('ARTICLE', ?, 1, now(), now())
                on conflict (target_type, target_id) do update
                set view_count = content_statistics.view_count + 1,
                    last_viewed_at = now(),
                    updated_at = now()
                """,
                articleId
        );
    }

    // 构造公开文章基础查询条件，复用列表和详情的可见性规则。
    private LambdaQueryWrapper<ArticleEntity> publicArticleQuery() {
        return new LambdaQueryWrapper<ArticleEntity>()
                .eq(ArticleEntity::getStatus, ContentConstants.STATUS_PUBLISHED)
                .eq(ArticleEntity::getPrivacyType, ContentConstants.PRIVACY_PUBLIC);
    }

    // 确认内容标识未被占用。
    private void ensureSlugAvailable(String slug, Long ignoredId) {
        LambdaQueryWrapper<ArticleEntity> query = new LambdaQueryWrapper<ArticleEntity>().eq(ArticleEntity::getSlug, slug);
        if (ignoredId != null) {
            query.ne(ArticleEntity::getId, ignoredId);
        }
        Long count = articleMapper.selectCount(query);
        if (count > 0) {
            throw BusinessException.conflict("文章标识已存在");
        }
    }

    // 查询必须存在的文章。
    private ArticleEntity requiredArticle(Long id) {
        ArticleEntity article = articleMapper.selectById(id);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        return article;
    }

    // 校验文章分类存在且属于文章模块，避免把数据库外键异常暴露给接口调用方。
    private void validateCategoryForArticle(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CategoryVO category = categoryService.findById(categoryId);
        if (!ContentConstants.MODULE_ARTICLE.equals(category.module())) {
            throw BusinessException.badRequest("文章分类不属于文章模块");
        }
    }

    // 校验标签集合存在，提前返回清晰业务错误。
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
    private void replaceTags(Long articleId, List<Long> tagIds) {
        articleTagMapper.deleteByArticleId(articleId);
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            articleTagMapper.insertIgnore(articleId, tagId);
        }
    }

    // 将请求中可编辑字段写回实体。
    private void applyEditableFields(ArticleEntity article, ArticleCreateRequest request, String slug, String privacyType) {
        article.setTitle(request.title().trim());
        article.setSlug(slug);
        article.setSummary(blankToNull(request.summary()));
        article.setContentMarkdown(request.contentMarkdown().trim());
        article.setCoverUrl(normalizeCoverUrl(request.coverUrl()));
        article.setCategoryId(request.categoryId());
        article.setPrivacyType(privacyType);
    }

    // 根据可见性推导已发布内容的真实状态。
    private String publishedStatusFor(String privacyType) {
        return ContentConstants.PRIVACY_PUBLIC.equals(privacyType)
                ? ContentConstants.STATUS_PUBLISHED
                : ContentConstants.STATUS_PRIVATE;
    }

    // 转换为前端可用的视图对象。
    private ArticleVO toVO(ArticleEntity entity, boolean includeContent) {
        CategoryVO category = entity.getCategoryId() == null ? null : categoryService.findById(entity.getCategoryId());
        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(entity.getId());
        List<TagVO> tags = tagIds.isEmpty() ? Collections.emptyList() : tagService.listByIds(tagIds);
        return new ArticleVO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getSummary(),
                includeContent ? entity.getContentMarkdown() : null,
                entity.getCoverUrl(),
                entity.getStatus(),
                entity.getPrivacyType(),
                entity.getTop(),
                entity.getRecommend(),
                category,
                tags,
                entity.getPublishTime(),
                entity.getCreatedBy(),
                entity.getSubmittedAt(),
                entity.getReviewedAt(),
                entity.getReviewNote()
        );
    }

    // 查询必须属于当前创作者的文章。
    private ArticleEntity requiredOwnedArticle(Long id, Long userId) {
        ArticleEntity article = requiredArticle(id);
        if (!Objects.equals(article.getCreatedBy(), userId)) {
            throw BusinessException.forbidden("只能操作自己的文章");
        }
        return article;
    }

    // 创作者只允许编辑未进入公开传播的文章。
    private void ensureCreatorEditable(ArticleEntity article) {
        if (!Set.of(ContentConstants.STATUS_DRAFT, ContentConstants.STATUS_REJECTED).contains(article.getStatus())) {
            throw BusinessException.badRequest("当前状态不能编辑，请等待审核或联系管理员");
        }
    }

    // 规范化并校验文章可见性。
    private String normalizePrivacyType(String privacyType) {
        String normalized = privacyType == null ? "" : privacyType.trim().toUpperCase();
        if (!ContentConstants.ARTICLE_PRIVACY_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("文章可见性不合法");
        }
        return normalized;
    }

    // 规范化可选文章状态筛选。
    private String normalizeOptionalStatus(String status) {
        String normalized = status == null || status.isBlank() ? "" : status.trim().toUpperCase();
        if (!normalized.isEmpty() && !ContentConstants.ARTICLE_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest("文章状态不合法");
        }
        return normalized;
    }

    // 规范化 URL 标识。
    private String normalizeSlug(String slug) {
        String normalized = slug.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw BusinessException.badRequest("文章标识只允许小写字母、数字和短横线");
        }
        return normalized;
    }

    // 文章封面只允许安全外链或站内上传资源。
    private String normalizeCoverUrl(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("/uploads/")) {
            return normalized;
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return normalized;
            }
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest("文章封面格式不合法");
        }
        throw BusinessException.badRequest("文章封面只允许 http、https 或站内上传地址");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
