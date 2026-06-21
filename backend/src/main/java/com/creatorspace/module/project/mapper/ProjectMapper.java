package com.creatorspace.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorspace.module.project.entity.ProjectEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

/**
 * 作品表数据访问接口。
 */
public interface ProjectMapper extends BaseMapper<ProjectEntity> {

    @Insert("""
            insert into portfolio_projects (
                title,
                slug,
                description,
                cover_url,
                project_type,
                tech_stack,
                github_url,
                demo_url,
                video_url,
                content_markdown,
                status,
                is_recommend,
                sort_order,
                submitted_at,
                reviewed_by,
                reviewed_at,
                review_note,
                created_by,
                updated_by
            )
            values (
                #{title},
                #{slug},
                #{description},
                #{coverUrl},
                #{projectType},
                cast(#{techStackJson} as jsonb),
                #{githubUrl},
                #{demoUrl},
                #{videoUrl},
                #{contentMarkdown},
                #{status},
                #{recommend},
                #{sortOrder},
                #{submittedAt},
                #{reviewedBy},
                #{reviewedAt},
                #{reviewNote},
                #{createdBy},
                #{updatedBy}
            )
            """)
    // 插入作品并写入 JSONB 技术栈。
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertProject(ProjectEntity project);

    @Update("""
            update portfolio_projects
            set title = #{title},
                slug = #{slug},
                description = #{description},
                cover_url = #{coverUrl},
                project_type = #{projectType},
                tech_stack = cast(#{techStackJson} as jsonb),
                github_url = #{githubUrl},
                demo_url = #{demoUrl},
                video_url = #{videoUrl},
                content_markdown = #{contentMarkdown},
                status = #{status},
                is_recommend = #{recommend},
                sort_order = #{sortOrder},
                submitted_at = #{submittedAt},
                reviewed_by = #{reviewedBy},
                reviewed_at = #{reviewedAt},
                review_note = #{reviewNote},
                updated_by = #{updatedBy},
                updated_at = now()
            where id = #{id}
            """)
    // 显式更新作品，保证 JSONB 技术栈和可清空字段都能正确写入。
    int updateProject(ProjectEntity project);

    @Update("""
            update portfolio_projects
            set status = #{status},
                submitted_at = #{submittedAt},
                reviewed_by = #{reviewedBy},
                reviewed_at = #{reviewedAt},
                review_note = #{reviewNote},
                updated_by = #{operatorId},
                updated_at = now()
            where id = #{id}
            """)
    // 更新作品投稿审核状态。
    int updateReviewState(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("submittedAt") OffsetDateTime submittedAt,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewedAt") OffsetDateTime reviewedAt,
            @Param("reviewNote") String reviewNote,
            @Param("operatorId") Long operatorId
    );
}
