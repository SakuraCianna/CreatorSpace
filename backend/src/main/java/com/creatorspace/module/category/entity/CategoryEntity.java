package com.creatorspace.module.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 分类表实体，用于区分文章、作品和灵感等内容模块。
 */
@TableName("categories")
public class CategoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;

    private String name;

    private String slug;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    private Boolean enabled;

    // 获取id。
    public Long getId() {
        return id;
    }

    // 设置id。
    public void setId(Long id) {
        this.id = id;
    }

    // 获取module。
    public String getModule() {
        return module;
    }

    // 设置module。
    public void setModule(String module) {
        this.module = module;
    }

    // 获取name。
    public String getName() {
        return name;
    }

    // 设置name。
    public void setName(String name) {
        this.name = name;
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

    // 获取sort order。
    public Integer getSortOrder() {
        return sortOrder;
    }

    // 设置sort order。
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    // 获取enabled。
    public Boolean getEnabled() {
        return enabled;
    }

    // 设置enabled。
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
