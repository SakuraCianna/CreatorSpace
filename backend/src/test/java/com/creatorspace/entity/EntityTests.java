package com.creatorspace.entity;

import com.creatorspace.module.article.entity.ArticleEntity;
import com.creatorspace.module.project.entity.ProjectEntity;
import com.creatorspace.module.category.entity.CategoryEntity;
import com.creatorspace.module.tag.entity.TagEntity;
import com.creatorspace.module.user.entity.UserEntity;
import com.creatorspace.module.user.entity.RoleEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTests {

    @Test
    void testArticleEntity() {
        ArticleEntity entity = new ArticleEntity();
        entity.setId(1L);
        entity.setTitle("Title");
        
        assertEquals(1L, entity.getId());
        assertEquals("Title", entity.getTitle());
    }

    @Test
    void testProjectEntity() {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(1L);
        entity.setTitle("Project Title");
        entity.setDescription("Desc");
        entity.setContentMarkdown("Content");
        entity.setDemoUrl("http://demo");
        entity.setCoverUrl("http://cover");
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(1L);
        entity.setUpdatedBy(2L);

        assertEquals(1L, entity.getId());
        assertEquals("Project Title", entity.getTitle());
        assertEquals("Desc", entity.getDescription());
        assertEquals("Content", entity.getContentMarkdown());
        assertEquals("http://demo", entity.getDemoUrl());
        assertEquals("http://cover", entity.getCoverUrl());
        assertEquals("ACTIVE", entity.getStatus());
        assertEquals(1L, entity.getCreatedBy());
        assertEquals(2L, entity.getUpdatedBy());
    }

    @Test
    void testCategoryEntity() {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(1L);
        entity.setName("Category");
        entity.setSlug("category");
        entity.setDescription("Desc");
        entity.setSortOrder(1);

        assertEquals(1L, entity.getId());
        assertEquals("Category", entity.getName());
        assertEquals("category", entity.getSlug());
        assertEquals("Desc", entity.getDescription());
        assertEquals(1, entity.getSortOrder());
    }

    @Test
    void testTagEntity() {
        TagEntity entity = new TagEntity();
        entity.setId(1L);
        entity.setName("Tag");
        entity.setSlug("tag");

        assertEquals(1L, entity.getId());
        assertEquals("Tag", entity.getName());
        assertEquals("tag", entity.getSlug());
    }

    @Test
    void testUserEntity() {
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setUsername("user");
        entity.setPasswordHash("hash");
        entity.setNickname("nickname");
        entity.setStatus("ACTIVE");

        assertEquals(1L, entity.getId());
        assertEquals("user", entity.getUsername());
        assertEquals("hash", entity.getPasswordHash());
        assertEquals("nickname", entity.getNickname());
        assertEquals("ACTIVE", entity.getStatus());
    }

    @Test
    void testRoleEntity() {
        RoleEntity entity = new RoleEntity();
        entity.setId(1L);
        entity.setName("ADMIN");

        assertEquals(1L, entity.getId());
        assertEquals("ADMIN", entity.getName());
    }
}
