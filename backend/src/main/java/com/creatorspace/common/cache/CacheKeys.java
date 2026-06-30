package com.creatorspace.common.cache;

/**
 * Redis key 命名空间，集中管理短缓存和访问去重标记。
 */
public final class CacheKeys {

    public static final String DASHBOARD_OVERVIEW = "creatorspace:cache:dashboard:overview";
    public static final String HOT_ARTICLES = "creatorspace:cache:dashboard:hot-articles";
    public static final String HOT_PROJECTS = "creatorspace:cache:dashboard:hot-projects";
    public static final String HOT_SEARCH_KEYWORDS = "creatorspace:cache:dashboard:hot-search-keywords";
    public static final String VISIT_DEDUPE_PREFIX = "creatorspace:dedupe:visit:";

    private CacheKeys() {
    }
}