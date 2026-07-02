package com.creatorspace.module.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.creatorspace.common.constant.ContentConstants;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;
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
    void setUp() {
        articleService = new ArticleServiceImpl(articleMapper, articleTagMapper, categoryService, tagService, jdbcTemplate, visitLogService);
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
}
