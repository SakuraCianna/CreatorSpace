package com.creatorspace.module.article.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章与标签关联表数据访问接口。
 */
public interface ArticleTagMapper {

    @Insert("""
            insert into article_tags (article_id, tag_id)
            values (#{articleId}, #{tagId})
            on conflict (article_id, tag_id) do nothing
            """)
    // 插入关联记录并忽略重复数据。
    int insertIgnore(Long articleId, Long tagId);

    // 删除文章已有标签绑定。
    @Delete("delete from article_tags where article_id = #{articleId}")
    int deleteByArticleId(Long articleId);

    @Select("""
            select tag_id
            from article_tags
            where article_id = #{articleId}
            order by tag_id
            """)
    // 查询文章绑定的标签 ID。
    List<Long> selectTagIdsByArticleId(Long articleId);
}
