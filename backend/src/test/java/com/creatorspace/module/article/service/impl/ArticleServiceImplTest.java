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
import com.creatorspace.module.article.vo.ArticleVO;
import com.creatorspace.module.category.service.CategoryService;
import com.creatorspace.module.category.vo.CategoryVO;
import com.creatorspace.module.statistics.VisitLogService;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private ArticleTagMapper articleTagMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TagService tagService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private VisitLogService visitLogService;

    private ArticleServiceImpl articleService;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        articleService = new ArticleServiceImpl(articleMapper, articleTagMapper, categoryService, tagService, jdbcTemplate, visitLogService);
        lenient().when(jdbcTemplate.queryForMap(anyString(), anyLong()))
                .thenReturn(Map.of("username", "creator", "avatar_url", "/avatar.png", "bio", "bio"));
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(1);
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());
        lenient().when(articleTagMapper.selectTagIdsByArticleId(anyLong())).thenReturn(Collections.emptyList());
    }

    @Test
    void create_success() {
        when(articleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(categoryService.findById(2L)).thenReturn(new CategoryVO(2L, ContentConstants.MODULE_ARTICLE, "Article", "article", null, 1, true));
        when(tagService.listByIds(List.of(1L))).thenReturn(List.of(new TagVO(1L, "Java", "java", "#f00", 1)));
        doAnswer(invocation -> {
            ArticleEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            return 1;
        }).when(articleMapper).insert(any(ArticleEntity.class));

        ArticleVO result = articleService.create(validRequest(), 99L);

        ArgumentCaptor<ArticleEntity> captor = ArgumentCaptor.forClass(ArticleEntity.class);
        verify(articleMapper).insert(captor.capture());
        ArticleEntity saved = captor.getValue();
        assertEquals("Hello Article", saved.getTitle());
        assertEquals("hello-article", saved.getSlug());
        assertEquals(ContentConstants.STATUS_DRAFT, saved.getStatus());
        assertEquals(ContentConstants.PRIVACY_PUBLIC, saved.getPrivacyType());
        assertEquals(99L, saved.getCreatedBy());
        verify(articleTagMapper).insertIgnore(12L, 1L);
        verify(jdbcTemplate).update(contains("insert into article_versions"), eq(12L), eq(1), eq("Hello Article"), eq("summary"), eq("body"), eq(99L));
        assertEquals(12L, result.id());
    }

    @Test
    void getAdminById_success() {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(1L);
        entity.setTitle("Test Title");
        entity.setCategoryId(2L);

        when(articleMapper.selectById(1L)).thenReturn(entity);
        when(categoryService.findById(2L)).thenReturn(new CategoryVO(2L, "module", "Cat", "cat", "desc", 1, true));
        when(articleTagMapper.selectTagIdsByArticleId(1L)).thenReturn(Collections.emptyList());

        ArticleVO result = articleService.getAdminById(1L);

        assertNotNull(result);
        assertEquals("Test Title", result.title());
        assertEquals(2L, result.category().id());
    }

    @Test
    void delete_success() {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(entity);

        articleService.delete(1L);

        verify(articleTagMapper).deleteByArticleId(1L);
        verify(articleMapper).deleteById(1L);
    }

    @Test
    void submitForReview_successAndSensitiveWordFailure() {
        ArticleEntity entity = article(3L, 88L, ContentConstants.STATUS_DRAFT);
        when(articleMapper.selectById(3L)).thenReturn(entity);

        ArticleVO result = articleService.submitForReview(3L, 88L);

        assertEquals(ContentConstants.STATUS_PENDING_REVIEW, result.status());
        assertNotNull(entity.getSubmittedAt());
        verify(articleMapper).updateReviewState(eq(3L), eq(ContentConstants.STATUS_PENDING_REVIEW),
                any(OffsetDateTime.class), isNull(), isNull(), isNull(), isNull(), eq(88L));

        ArticleEntity blocked = article(4L, 88L, ContentConstants.STATUS_DRAFT);
        blocked.setContentMarkdown("contains blocked text");
        when(articleMapper.selectById(4L)).thenReturn(blocked);
        when(jdbcTemplate.query(contains("sensitive_words"), any(RowMapper.class))).thenReturn(List.of("blocked|EXACT"));

        BusinessException exception = assertThrows(BusinessException.class, () -> articleService.submitForReview(4L, 88L));
        assertEquals("文章包含敏感内容，请修改后重新提交", exception.getMessage());
    }

    @Test
    void approveRejectAndUnpublishChangeReviewState() {
        ArticleEntity entity = article(5L, 11L, ContentConstants.STATUS_PENDING_REVIEW);
        when(articleMapper.selectById(5L)).thenReturn(entity);

        ArticleVO approved = articleService.approve(5L, 1L);
        assertEquals(ContentConstants.STATUS_PUBLISHED, approved.status());
        assertEquals(1L, entity.getReviewedBy());
        assertNotNull(entity.getPublishTime());

        ArticleEntity privateArticle = article(6L, 11L, ContentConstants.STATUS_PENDING_REVIEW);
        privateArticle.setPrivacyType("SELF");
        when(articleMapper.selectById(6L)).thenReturn(privateArticle);
        ArticleVO privateResult = articleService.approve(6L, 1L);
        assertEquals(ContentConstants.STATUS_PRIVATE, privateResult.status());

        ArticleEntity rejected = article(7L, 11L, ContentConstants.STATUS_PENDING_REVIEW);
        when(articleMapper.selectById(7L)).thenReturn(rejected);
        ArticleVO rejectedResult = articleService.reject(7L, " ", 1L);
        assertEquals(ContentConstants.STATUS_REJECTED, rejectedResult.status());
        assertEquals("内容暂未通过审核，请修改后重新提交。", rejectedResult.reviewNote());

        ArticleEntity published = article(8L, 11L, ContentConstants.STATUS_PUBLISHED);
        when(articleMapper.selectById(8L)).thenReturn(published);
        ArticleVO draft = articleService.unpublish(8L, 1L);
        assertEquals(ContentConstants.STATUS_DRAFT, draft.status());
    }

    @Test
    void updateMineRejectsForeignOrPublishedArticle() {
        when(articleMapper.selectById(10L)).thenReturn(article(10L, 1L, ContentConstants.STATUS_DRAFT));
        assertThrows(BusinessException.class, () -> articleService.updateMine(10L, validRequest(), 2L));

        when(articleMapper.selectById(11L)).thenReturn(article(11L, 1L, ContentConstants.STATUS_PUBLISHED));
        assertThrows(BusinessException.class, () -> articleService.updateMine(11L, validRequest(), 1L));
    }

    @Test
    void createRejectsInvalidSlugPrivacyCategoryAndTags() {
        ArticleCreateRequest badSlug = new ArticleCreateRequest("Title", "Bad Slug", "summary", "body", null, null, List.of(), "PUBLIC");
        assertThrows(BusinessException.class, () -> articleService.create(badSlug, 1L));

        ArticleCreateRequest badPrivacy = new ArticleCreateRequest("Title", "good-slug", "summary", "body", null, null, List.of(), "BAD");
        assertThrows(BusinessException.class, () -> articleService.create(badPrivacy, 1L));

        when(articleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(categoryService.findById(9L)).thenReturn(new CategoryVO(9L, ContentConstants.MODULE_PROJECT, "Project", "project", null, 1, true));
        ArticleCreateRequest badCategory = new ArticleCreateRequest("Title", "good-slug", "summary", "body", null, 9L, List.of(), "PUBLIC");
        assertThrows(BusinessException.class, () -> articleService.create(badCategory, 1L));

        ArticleCreateRequest badTags = new ArticleCreateRequest("Title", "good-slug", "summary", "body", null, null, Arrays.asList(1L, null), "PUBLIC");
        assertThrows(BusinessException.class, () -> articleService.create(badTags, 1L));
    }

    @Test
    void listPublicByUnknownTagReturnsEmptyPage() {
        when(articleTagMapper.selectArticleIdsByTagId(404L)).thenReturn(Collections.emptyList());

        PageResponse<ArticleVO> result = articleService.listPublic("", 404L, 1, 20);

        assertEquals(0, result.total());
        assertTrue(result.records().isEmpty());
        verify(articleMapper, never()).selectPage(any(), any());
    }

    @Test
    void setTop_success() {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(entity);

        articleService.setTop(1L, true, 99L);

        verify(articleMapper).updateById(entity);
        assertTrue(entity.getTop());
        assertEquals(99L, entity.getUpdatedBy());
    }

    @Test
    void setRecommend_success() {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(entity);

        articleService.setRecommend(1L, true, 99L);

        verify(articleMapper).updateById(entity);
        assertTrue(entity.getRecommend());
        assertEquals(99L, entity.getUpdatedBy());
    }

    private ArticleCreateRequest validRequest() {
        return new ArticleCreateRequest(
                " Hello Article ",
                "Hello-Article",
                " summary ",
                " body ",
                "/uploads/article/cover.png",
                2L,
                List.of(1L),
                " public "
        );
    }

    private ArticleEntity article(Long id, Long ownerId, String status) {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(id);
        entity.setTitle("Article " + id);
        entity.setSlug("article-" + id);
        entity.setSummary("summary");
        entity.setContentMarkdown("body");
        entity.setCoverUrl("/uploads/article/cover.png");
        entity.setCategoryId(null);
        entity.setStatus(status);
        entity.setPrivacyType(ContentConstants.PRIVACY_PUBLIC);
        entity.setViewCount(0L);
        entity.setLikeCount(0L);
        entity.setCommentCount(0L);
        entity.setTop(false);
        entity.setRecommend(false);
        entity.setCreatedBy(ownerId);
        entity.setUpdatedBy(ownerId);
        return entity;
    }
}
