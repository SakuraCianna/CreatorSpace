package com.creatorspace.module.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.entity.TagEntity;
import com.creatorspace.module.tag.mapper.TagMapper;
import com.creatorspace.module.tag.vo.TagVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagServiceImpl(tagMapper, jdbcTemplate);
    }

    @Test
    void create_success() {
        TagRequest request = new TagRequest("Test Tag", "test-tag", "#ff0000", 10);
        when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        TagVO result = tagService.create(request);

        assertNotNull(result);
        assertEquals("Test Tag", result.name());
        assertEquals("test-tag", result.slug());

        ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
        verify(tagMapper).insert(captor.capture());
        assertEquals("Test Tag", captor.getValue().getName());
    }

    @Test
    void update_success() {
        TagRequest request = new TagRequest("Updated Tag", "updated-tag", null, 0);
        TagEntity existing = new TagEntity();
        existing.setId(1L);
        existing.setName("Old Tag");

        when(tagMapper.selectById(1L)).thenReturn(existing);
        when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        TagVO result = tagService.update(1L, request);

        assertNotNull(result);
        assertEquals("Updated Tag", result.name());
        verify(tagMapper).updateById(existing);
    }

    @Test
    void delete_success() {
        TagEntity existing = new TagEntity();
        existing.setId(1L);

        when(tagMapper.selectById(1L)).thenReturn(existing);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(0L);

        tagService.delete(1L);

        verify(tagMapper).deleteById(1L);
    }

    @Test
    void list_success() {
        TagEntity tag1 = new TagEntity();
        tag1.setId(1L);
        tag1.setName("Tag 1");
        tag1.setSlug("tag-1");
        when(tagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tag1));

        List<TagVO> result = tagService.list();

        assertEquals(1, result.size());
        assertEquals("Tag 1", result.get(0).name());
    }
}
