package com.creatorspace.module.tag.vo;

/**
 * 返回给前端的标签视图对象。
 */
public record TagVO(Long id, String name, String slug, String color, Integer weight) {
}
