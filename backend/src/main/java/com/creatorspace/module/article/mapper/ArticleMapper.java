package com.creatorspace.module.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorspace.module.article.entity.ArticleEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文章表基础数据访问接口。
 */
public interface ArticleMapper extends BaseMapper<ArticleEntity> {

    @Update("""
            update articles
            set title = #{title},
                slug = #{slug},
                summary = #{summary},
                content_markdown = #{contentMarkdown},
                cover_url = #{coverUrl},
                category_id = #{categoryId},
                status = #{status},
                privacy_type = #{privacyType},
                updated_by = #{updatedBy},
                updated_at = now()
            where id = #{id}
            """)
    // 显式更新可编辑字段，允许摘要、封面和分类被清空。
    int updateEditableArticle(ArticleEntity article);

    @Update("""
            update articles
            set status = #{status},
                publish_time = #{publishTime},
                updated_by = #{operatorId},
                updated_at = now()
            where id = #{id}
            """)
    // 更新发布状态，允许撤回时清空发布时间。
    int updatePublishState(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("publishTime") java.time.OffsetDateTime publishTime,
            @Param("operatorId") Long operatorId
    );
}
