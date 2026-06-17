package com.creatorspace.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorspace.module.project.entity.ProjectEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

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
                #{createdBy},
                #{updatedBy}
            )
            """)
    // 插入作品并写入 JSONB 技术栈。
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertProject(ProjectEntity project);
}
