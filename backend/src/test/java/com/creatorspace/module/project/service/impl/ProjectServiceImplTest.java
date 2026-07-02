package com.creatorspace.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.constant.ContentConstants;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.project.dto.ProjectCreateRequest;
import com.creatorspace.module.project.entity.ProjectEntity;
import com.creatorspace.module.project.mapper.ProjectMapper;
import com.creatorspace.module.project.mapper.ProjectTagMapper;
import com.creatorspace.module.project.vo.ProjectVO;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectTagMapper projectTagMapper;
    @Mock
    private TagService tagService;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private VisitLogService visitLogService;

    private ProjectServiceImpl projectService;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        projectService = new ProjectServiceImpl(projectMapper, projectTagMapper, tagService, jdbcTemplate, visitLogService);
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(projectTagMapper.selectTagIdsByProjectId(anyLong())).thenReturn(Collections.emptyList());
    }

    @Test
    void createMineSavesDraftAndTags() {
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(tagService.listByIds(List.of(1L, 2L))).thenReturn(List.of(
                new TagVO(1L, "Vue", "vue", "#42b883", 1),
                new TagVO(2L, "Spring", "spring", "#6db33f", 1)
        ));
        doAnswer(invocation -> {
            ProjectEntity project = invocation.getArgument(0);
            project.setId(10L);
            return 1;
        }).when(projectMapper).insertProject(any(ProjectEntity.class));

        ProjectVO result = projectService.createMine(validRequest(), 99L);

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectMapper).insertProject(captor.capture());
        ProjectEntity saved = captor.getValue();
        assertEquals("Creator Portfolio", saved.getTitle());
        assertEquals("creator-portfolio", saved.getSlug());
        assertEquals(ContentConstants.PROJECT_DRAFT, saved.getStatus());
        assertFalse(saved.getRecommend());
        assertEquals(99L, saved.getCreatedBy());
        assertEquals("[\"Vue\",\"Spring Boot\"]", saved.getTechStackJson());
        assertEquals(10L, result.id());
        verify(projectTagMapper).insertIgnore(10L, 1L);
        verify(projectTagMapper).insertIgnore(10L, 2L);
    }

    @Test
    void submitForReviewMovesOwnedDraftToPending() {
        ProjectEntity existing = project(8L, 77L, ContentConstants.PROJECT_DRAFT);
        when(projectMapper.selectById(8L)).thenReturn(existing);

        ProjectVO result = projectService.submitForReview(8L, 77L);

        assertEquals(ContentConstants.PROJECT_PENDING_REVIEW, result.status());
        assertNotNull(existing.getSubmittedAt());
        verify(projectMapper).updateReviewState(eq(8L), eq(ContentConstants.PROJECT_PENDING_REVIEW),
                any(OffsetDateTime.class), isNull(), isNull(), isNull(), eq(77L));
    }

    @Test
    void approvePublishesProjectAndRejectUsesDefaultNote() {
        ProjectEntity project = project(3L, 20L, ContentConstants.PROJECT_PENDING_REVIEW);
        when(projectMapper.selectById(3L)).thenReturn(project);

        ProjectVO approved = projectService.approve(3L, 1L);
        assertEquals(ContentConstants.PROJECT_VISIBLE, approved.status());
        assertEquals(1L, project.getReviewedBy());

        ProjectEntity second = project(4L, 20L, ContentConstants.PROJECT_PENDING_REVIEW);
        when(projectMapper.selectById(4L)).thenReturn(second);
        ProjectVO rejected = projectService.reject(4L, " ", 1L);
        assertEquals(ContentConstants.PROJECT_REJECTED, rejected.status());
        assertEquals("作品暂未通过审核，请修改后重新提交。", rejected.reviewNote());
    }

    @Test
    void setRecommendAndStatusUpdateProject() {
        ProjectEntity project = project(5L, 20L, ContentConstants.PROJECT_VISIBLE);
        when(projectMapper.selectById(5L)).thenReturn(project);

        ProjectVO recommended = projectService.setRecommend(5L, true, 1L);
        ProjectVO hidden = projectService.setStatus(5L, "hidden", 1L);

        assertTrue(recommended.recommended());
        assertEquals(ContentConstants.PROJECT_HIDDEN, hidden.status());
        verify(projectMapper, times(2)).updateProject(project);
    }

    @Test
    void deleteMineRejectsVisibleOrForeignProject() {
        when(projectMapper.selectById(1L)).thenReturn(project(1L, 10L, ContentConstants.PROJECT_VISIBLE));
        assertThrows(BusinessException.class, () -> projectService.deleteMine(1L, 10L));

        when(projectMapper.selectById(2L)).thenReturn(project(2L, 10L, ContentConstants.PROJECT_DRAFT));
        assertThrows(BusinessException.class, () -> projectService.deleteMine(2L, 99L));

        verify(projectMapper, never()).deleteById(anyLong());
    }

    @Test
    void createRejectsDuplicateSlugMissingTagAndInvalidUrl() {
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(BusinessException.class, () -> projectService.createMine(validRequest(), 1L));

        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(tagService.listByIds(anyList())).thenReturn(List.of(new TagVO(1L, "Vue", "vue", null, 1)));
        assertThrows(BusinessException.class, () -> projectService.createMine(validRequest(), 1L));

        ProjectCreateRequest invalidUrl = new ProjectCreateRequest(
                "Bad", "bad-project", "desc", "ftp://example.com/a.png", "Web",
                List.of(), null, null, null, "body", List.of(), false);
        assertThrows(BusinessException.class, () -> projectService.createMine(invalidUrl, 1L));
    }

    @Test
    void getPublicBySlugRecordsUniqueVisitAndIncrementsStats() {
        ProjectEntity publicProject = project(6L, 10L, ContentConstants.PROJECT_VISIBLE);
        when(projectMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(publicProject);
        when(visitLogService.recordContentVisit(eq("PROJECT"), eq(6L), any())).thenReturn(true);

        ProjectVO result = projectService.getPublicBySlug("public-project", mock(jakarta.servlet.http.HttpServletRequest.class));

        assertEquals(6L, result.id());
        verify(jdbcTemplate).update(contains("insert into content_statistics"), eq(6L));
    }

    private ProjectCreateRequest validRequest() {
        return new ProjectCreateRequest(
                " Creator Portfolio ",
                "Creator-Portfolio",
                " personal works ",
                "/uploads/project/cover.png",
                " Web ",
                List.of("Vue", "Spring Boot"),
                "https://github.com/example/repo",
                "https://demo.example.com",
                "/uploads/project/demo.mp4",
                " # body ",
                List.of(1L, 2L),
                true
        );
    }

    private ProjectEntity project(Long id, Long ownerId, String status) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setTitle("Project " + id);
        entity.setSlug(id == 6L ? "public-project" : "project-" + id);
        entity.setDescription("desc");
        entity.setCoverUrl("/uploads/project/cover.png");
        entity.setProjectType("Web");
        entity.setTechStackJson("[\"Vue\"]");
        entity.setGithubUrl("https://github.com/example/repo");
        entity.setDemoUrl("https://demo.example.com");
        entity.setVideoUrl(null);
        entity.setContentMarkdown("body");
        entity.setStatus(status);
        entity.setRecommend(false);
        entity.setSortOrder(0);
        entity.setCreatedBy(ownerId);
        entity.setUpdatedBy(ownerId);
        return entity;
    }
}
