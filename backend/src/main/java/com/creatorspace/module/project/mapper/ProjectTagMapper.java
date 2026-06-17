package com.creatorspace.module.project.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作品与标签关联表数据访问接口。
 */
public interface ProjectTagMapper {

    @Insert("""
            insert into project_tags (project_id, tag_id)
            values (#{projectId}, #{tagId})
            on conflict (project_id, tag_id) do nothing
            """)
    // 插入关联记录并忽略重复数据。
    int insertIgnore(Long projectId, Long tagId);

    // 删除作品已有标签绑定。
    @Delete("delete from project_tags where project_id = #{projectId}")
    int deleteByProjectId(Long projectId);

    @Select("""
            select tag_id
            from project_tags
            where project_id = #{projectId}
            order by tag_id
            """)
    // 查询作品绑定的标签 ID。
    List<Long> selectTagIdsByProjectId(Long projectId);
}
