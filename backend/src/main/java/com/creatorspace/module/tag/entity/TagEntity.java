package com.creatorspace.module.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 标签表实体，用于文章和作品的轻量分类。
 */
@TableName("tags")
public class TagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String slug;

    private String color;

    private Integer weight;

    // 获取id。
    public Long getId() {
        return id;
    }

    // 设置id。
    public void setId(Long id) {
        this.id = id;
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

    // 获取color。
    public String getColor() {
        return color;
    }

    // 设置color。
    public void setColor(String color) {
        this.color = color;
    }

    // 获取weight。
    public Integer getWeight() {
        return weight;
    }

    // 设置weight。
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
