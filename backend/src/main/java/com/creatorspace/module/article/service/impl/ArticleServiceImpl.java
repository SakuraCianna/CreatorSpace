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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // 通过构造器注入文章、分类和标签协作服务。
    public ArticleServiceImpl(
            ArticleMapper articleMapper,
            ArticleTagMapper articleTagMapper,
            CategoryService categoryService,
            TagService tagService
    ) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    // 创建草稿文章，并在同一个事务内写入标签绑定。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO create(ArticleCreateRequest request, Long operatorId) {
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(slug);
        String privacyType = normalizePrivacyType(request.privacyType());
        validateCategoryForArticle(request.categoryId());
        validateTagIds(request.tagIds());

        ArticleEntity article = new ArticleEntity();
        article.setTitle(request.title().trim());
        article.setSlug(slug);
        article.setSummary(request.summary());
        article.setContentMarkdown(request.contentMarkdown());
        article.setCoverUrl(request.coverUrl());
        article.setCategoryId(request.categoryId());
        article.setStatus(ContentConstants.STATUS_DRAFT);
        article.setPrivacyType(privacyType);
        article.setTop(false);
        article.setRecommend(false);
        article.setCreatedBy(operatorId);
        article.setUpdatedBy(operatorId);
        articleMapper.insert(article);
        replaceTags(article.getId(), request.tagIds());
        return toVO(article, true);
    }

    // 发布文章时根据可见性计算最终状态，私密内容不会进入公开列表。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO publish(Long id, Long operatorId) {
        ArticleEntity article = requiredArticle(id);
        article.setStatus(ContentConstants.PRIVACY_PUBLIC.equals(article.getPrivacyType())
                ? ContentConstants.STATUS_PUBLISHED
                : ContentConstants.STATUS_PRIVATE);
        article.setPublishTime(OffsetDateTime.now());
        article.setUpdatedBy(operatorId);
        articleMapper.updateById(article);
        return toVO(article, true);
    }

    // 查询公开文章列表，只暴露已发布且公开可见的内容。
    @Override
    public PageResponse<ArticleVO> listPublic(String keyword, long page, long pageSize) {
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

    // 按 URL 标识读取公开文章详情。
    @Override
    public ArticleVO getPublicBySlug(String slug) {
        ArticleEntity article = articleMapper.selectOne(publicArticleQuery()
                .eq(ArticleEntity::getSlug, normalizeSlug(slug)));
        if (article == null) {
            throw BusinessException.notFound("文章不存在或不可见");
        }
        return toVO(article, true);
    }

    // 构造公开文章基础查询条件，复用列表和详情的可见性规则。
    private LambdaQueryWrapper<ArticleEntity> publicArticleQuery() {
        return new LambdaQueryWrapper<ArticleEntity>()
                .eq(ArticleEntity::getStatus, ContentConstants.STATUS_PUBLISHED)
                .eq(ArticleEntity::getPrivacyType, ContentConstants.PRIVACY_PUBLIC);
    }

    // 确认内容标识未被占用。
    private void ensureSlugAvailable(String slug) {
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<ArticleEntity>().eq(ArticleEntity::getSlug, slug));
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
                category,
                tags,
                entity.getPublishTime()
        );
    }

    // 规范化并校验文章可见性。
    private String normalizePrivacyType(String privacyType) {
        String normalized = privacyType == null ? "" : privacyType.trim().toUpperCase();
        if (!ContentConstants.ARTICLE_PRIVACY_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("文章可见性不合法");
        }
        return normalized;
    }

    // 规范化 URL 标识。
    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }
}
