package com.creatorspace.common.constant;

import java.util.Set;

/**
 * 集中维护第一阶段内容、角色和可见性常量，避免魔法值散落在业务代码里。
 */
public final class ContentConstants {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    public static final String USER_ACTIVE = "ACTIVE";

    public static final String MODULE_ARTICLE = "ARTICLE";
    public static final String MODULE_PROJECT = "PROJECT";
    public static final String MODULE_INSPIRATION = "INSPIRATION";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_PRIVATE = "PRIVATE";
    public static final String PROJECT_VISIBLE = "VISIBLE";

    public static final String PRIVACY_PUBLIC = "PUBLIC";

    public static final Set<String> CATEGORY_MODULES = Set.of(MODULE_ARTICLE, MODULE_PROJECT, MODULE_INSPIRATION);
    public static final Set<String> ARTICLE_PRIVACY_TYPES = Set.of(
            "PUBLIC",
            "SELF",
            "FRIENDS",
            "SELECTED_FRIENDS",
            "EXCLUDED_FRIENDS"
    );

    // 工具类不允许被实例化。
    private ContentConstants() {
    }
}
