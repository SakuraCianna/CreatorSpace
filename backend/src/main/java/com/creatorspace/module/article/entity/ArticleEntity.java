package com.creatorspace.module.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 文章表实体，保存正文、发布状态和可见性规则。
 */
@TableName("articles")
public class ArticleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String slug;

    private String summary;

    @TableField("content_markdown")
    private String contentMarkdown;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("category_id")
    private Long categoryId;

    private String status;

    @TableField("privacy_type")
    private String privacyType;

    @TableField("view_count")
    private Long viewCount;

    @TableField("like_count")
    private Long likeCount;

    @TableField("comment_count")
    private Long commentCount;

    @TableField("is_top")
    private Boolean top;

    @TableField("is_recommend")
    private Boolean recommend;

    @TableField("submitted_at")
    private OffsetDateTime submittedAt;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("reviewed_at")
    private OffsetDateTime reviewedAt;

    @TableField("review_note")
    private String reviewNote;

    @TableField("publish_time")
    private OffsetDateTime publishTime;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    // 获取id。
    public Long getId() {
        return id;
    }

    // 设置id。
    public void setId(Long id) {
        this.id = id;
    }

    // 获取title。
    public String getTitle() {
        return title;
    }

    // 设置title。
    public void setTitle(String title) {
        this.title = title;
    }

    // 获取slug。
    public String getSlug() {
        return slug;
    }

    // 设置slug。
    public void setSlug(String slug) {
        this.slug = slug;
    }

    // 获取summary。
    public String getSummary() {
        return summary;
    }

    // 设置summary。
    public void setSummary(String summary) {
        this.summary = summary;
    }

    // 获取content markdown。
    public String getContentMarkdown() {
        return contentMarkdown;
    }

    // 设置content markdown。
    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    // 获取cover url。
    public String getCoverUrl() {
        return coverUrl;
    }

    // 设置cover url。
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    // 获取category id。
    public Long getCategoryId() {
        return categoryId;
    }

    // 设置category id。
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    // 获取发布状态。
    public String getStatus() {
        return status;
    }

    // 设置发布状态。
    public void setStatus(String status) {
        this.status = status;
    }

    // 获取privacy type。
    public String getPrivacyType() {
        return privacyType;
    }

    // 设置privacy type。
    public void setPrivacyType(String privacyType) {
        this.privacyType = privacyType;
    }

    // 获取view count。
    public Long getViewCount() {
        return viewCount;
    }

    // 设置view count。
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    // 获取like count。
    public Long getLikeCount() {
        return likeCount;
    }

    // 设置like count。
    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    // 获取comment count。
    public Long getCommentCount() {
        return commentCount;
    }

    // 设置comment count。
    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    // 获取top。
    public Boolean getTop() {
        return top;
    }

    // 设置top。
    public void setTop(Boolean top) {
        this.top = top;
    }

    // 获取recommend。
    public Boolean getRecommend() {
        return recommend;
    }

    // 设置recommend。
    public void setRecommend(Boolean recommend) {
        this.recommend = recommend;
    }

    // 获取submitted at。
    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    // 设置submitted at。
    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    // 获取reviewed by。
    public Long getReviewedBy() {
        return reviewedBy;
    }

    // 设置reviewed by。
    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    // 获取reviewed at。
    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    // 设置reviewed at。
    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    // 获取review note。
    public String getReviewNote() {
        return reviewNote;
    }

    // 设置review note。
    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    // 获取publish time。
    public OffsetDateTime getPublishTime() {
        return publishTime;
    }

    // 设置publish time。
    public void setPublishTime(OffsetDateTime publishTime) {
        this.publishTime = publishTime;
    }

    // 获取created by。
    public Long getCreatedBy() {
        return createdBy;
    }

    // 设置created by。
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    // 获取updated by。
    public Long getUpdatedBy() {
        return updatedBy;
    }

    // 设置updated by。
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
