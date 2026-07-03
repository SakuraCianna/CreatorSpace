package com.creatorspace.module.user.vo;

/**
 * 用户博客外观配置, 前台只消费白名单字段生成局部 CSS 变量。
 */
public record BlogThemeVO(
        String displayName,
        String fontPreset,
        String accentColor,
        String titleColor,
        String bodyColor,
        String canvasType,
        String canvasColor,
        String canvasImage,
        String paperColor,
        String layoutStyle,
        String blockStyle
) {
}
