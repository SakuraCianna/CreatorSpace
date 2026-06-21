package com.creatorspace.module.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 作品表实体，保存项目展示信息、技术栈和可见状态。
 */
@TableName("portfolio_projects")
public class ProjectEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String slug;

    private String description;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("project_type")
    private String projectType;

    @TableField("tech_stack")
    private String techStackJson;

    @TableField("github_url")
    private String githubUrl;

    @TableField("demo_url")
    private String demoUrl;

    @TableField("video_url")
    private String videoUrl;

    @TableField("content_markdown")
    private String contentMarkdown;

    private String status;

    @TableField("is_recommend")
    private Boolean recommend;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("submitted_at")
    private OffsetDateTime submittedAt;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("reviewed_at")
    private OffsetDateTime reviewedAt;

    @TableField("review_note")
    private String reviewNote;

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

    // 获取description。
    public String getDescription() {
        return description;
    }

    // 设置description。
    public void setDescription(String description) {
        this.description = description;
    }

    // 获取cover url。
    public String getCoverUrl() {
        return coverUrl;
    }

    // 设置cover url。
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    // 获取project type。
    public String getProjectType() {
        return projectType;
    }

    // 设置project type。
    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    // 获取tech stack json。
    public String getTechStackJson() {
        return techStackJson;
    }

    // 设置tech stack json。
    public void setTechStackJson(String techStackJson) {
        this.techStackJson = techStackJson;
    }

    // 获取github url。
    public String getGithubUrl() {
        return githubUrl;
    }

    // 设置github url。
    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    // 获取demo url。
    public String getDemoUrl() {
        return demoUrl;
    }

    // 设置demo url。
    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
    }

    // 获取video url。
    public String getVideoUrl() {
        return videoUrl;
    }

    // 设置video url。
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    // 获取content markdown。
    public String getContentMarkdown() {
        return contentMarkdown;
    }

    // 设置content markdown。
    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    // 获取作品状态。
    public String getStatus() {
        return status;
    }

    // 设置作品状态。
    public void setStatus(String status) {
        this.status = status;
    }

    // 获取recommend。
    public Boolean getRecommend() {
        return recommend;
    }

    // 设置recommend。
    public void setRecommend(Boolean recommend) {
        this.recommend = recommend;
    }

    // 获取sort order。
    public Integer getSortOrder() {
        return sortOrder;
    }

    // 设置sort order。
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
