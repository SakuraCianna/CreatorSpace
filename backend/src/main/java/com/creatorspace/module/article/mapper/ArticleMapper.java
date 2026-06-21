package com.creatorspace.module.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.creatorspace.module.article.entity.ArticleEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

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
                submitted_at = #{submittedAt},
                reviewed_by = #{reviewedBy},
                reviewed_at = #{reviewedAt},
                review_note = #{reviewNote},
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
            @Param("publishTime") OffsetDateTime publishTime,
            @Param("operatorId") Long operatorId
    );

    @Update("""
            update articles
            set status = #{status},
                submitted_at = #{submittedAt},
                reviewed_by = #{reviewedBy},
                reviewed_at = #{reviewedAt},
                review_note = #{reviewNote},
                publish_time = #{publishTime},
                updated_by = #{operatorId},
                updated_at = now()
            where id = #{id}
            """)
    // 更新投稿审核状态，覆盖提交、通过、驳回和撤回审核。
    int updateReviewState(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("submittedAt") OffsetDateTime submittedAt,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewedAt") OffsetDateTime reviewedAt,
            @Param("reviewNote") String reviewNote,
            @Param("publishTime") OffsetDateTime publishTime,
            @Param("operatorId") Long operatorId
    );
}
