package com.creatorspace.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.category.dto.CategoryRequest;
import com.creatorspace.module.category.entity.CategoryEntity;
import com.creatorspace.module.category.mapper.CategoryMapper;
import com.creatorspace.module.category.vo.CategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryMapper);
    }

    @Test
    void createNormalizesInputAndDefaultsEnabled() {
        when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        CategoryRequest request = new CategoryRequest(" article ", " 技术文章 ", "Java-Notes", "  desc  ", null, null);

        CategoryVO result = categoryService.create(request);

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(categoryMapper).insert(captor.capture());
        CategoryEntity saved = captor.getValue();
        assertEquals("ARTICLE", saved.getModule());
        assertEquals("技术文章", saved.getName());
        assertEquals("java-notes", saved.getSlug());
        assertEquals("desc", saved.getDescription());
        assertEquals(0, saved.getSortOrder());
        assertTrue(saved.getEnabled());
        assertEquals("java-notes", result.slug());
    }

    @Test
    void createRejectsDuplicateSlug() {
        when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.create(new CategoryRequest("ARTICLE", "Name", "name", null, 1, true)));

        assertEquals("分类标识已存在", exception.getMessage());
        verify(categoryMapper, never()).insert(any(CategoryEntity.class));
    }

    @Test
    void updateKeepsExistingEnabledWhenRequestNull() {
        CategoryEntity existing = category(8L, "ARTICLE", true);
        when(categoryMapper.selectById(8L)).thenReturn(existing);
        when(categoryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        CategoryVO result = categoryService.update(8L,
                new CategoryRequest("PROJECT", "作品", "portfolio", "desc", 5, null));

        assertEquals("PROJECT", existing.getModule());
        assertEquals("portfolio", existing.getSlug());
        assertTrue(existing.getEnabled());
        assertEquals(5, result.sortOrder());
        verify(categoryMapper).updateById(existing);
    }

    @Test
    void setEnabledUpdatesState() {
        CategoryEntity existing = category(2L, "ARTICLE", true);
        when(categoryMapper.selectById(2L)).thenReturn(existing);

        CategoryVO result = categoryService.setEnabled(2L, false);

        assertFalse(result.enabled());
        verify(categoryMapper).updateById(existing);
    }

    @Test
    void listEnabledFiltersByModule() {
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(category(1L, "ARTICLE", true)));

        List<CategoryVO> result = categoryService.listEnabled("article");

        assertEquals(1, result.size());
        assertEquals("ARTICLE", result.getFirst().module());
    }

    @Test
    void findByIdRejectsMissingOrDisabledCategory() {
        when(categoryMapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> categoryService.findById(1L));

        when(categoryMapper.selectById(2L)).thenReturn(category(2L, "ARTICLE", false));
        assertThrows(BusinessException.class, () -> categoryService.findById(2L));
    }

    @Test
    void rejectsInvalidModuleSlugAndBlankName() {
        assertThrows(BusinessException.class,
                () -> categoryService.create(new CategoryRequest("BAD", "Name", "name", null, 0, true)));
        assertThrows(BusinessException.class,
                () -> categoryService.create(new CategoryRequest("ARTICLE", "Name", "Bad Slug", null, 0, true)));
        assertThrows(BusinessException.class,
                () -> categoryService.create(new CategoryRequest("ARTICLE", " ", "name", null, 0, true)));
    }

    private CategoryEntity category(Long id, String module, boolean enabled) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setModule(module);
        entity.setName("Name " + id);
        entity.setSlug("name-" + id);
        entity.setDescription("desc");
        entity.setSortOrder(1);
        entity.setEnabled(enabled);
        return entity;
    }
}
