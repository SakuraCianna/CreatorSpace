package com.creatorspace.module.dashboard;

import com.creatorspace.common.cache.CacheKeys;
import com.creatorspace.common.cache.RedisJsonCacheService;
import com.creatorspace.common.result.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * 后台运营概览接口，提供 CMS 首页需要的内容、互动和访问摘要。
 */
@RestController
public class DashboardController {

    private static final Duration OVERVIEW_CACHE_TTL = Duration.ofSeconds(45);
    private static final Duration HOT_CONTENT_CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration HOT_SEARCH_CACHE_TTL = Duration.ofMinutes(2);

    private final JdbcTemplate jdbcTemplate;
    private final RedisJsonCacheService redisCacheService;

    // 通过聚合 SQL 生成后台概览，避免后台页面继续依赖硬编码样例。
    public DashboardController(JdbcTemplate jdbcTemplate, RedisJsonCacheService redisCacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisCacheService = redisCacheService;
    }

    // 返回后台仪表盘概览数据。
    @GetMapping("/api/admin/dashboard/overview")
    public ApiResponse<DashboardOverviewVO> overview() {
        return ApiResponse.ok(cached(CacheKeys.DASHBOARD_OVERVIEW, DashboardOverviewVO.class, OVERVIEW_CACHE_TTL, this::buildOverview));
    }

    private DashboardOverviewVO buildOverview() {
        return new DashboardOverviewVO(
                List.of(
                        metric("文章总数", count("articles"), "公开/私密/草稿统一管理"),
                        metric("作品总数", count("portfolio_projects"), "作品档案、截图和外链"),
                        metric("灵感总数", count("inspiration_cards"), "摘句、Prompt、链接和参考图"),
                        metric("评论总数", count("comments"), "全部评论记录"),
                        metric("留言总数", count("guestbook_entries"), "访客留言与审核状态"),
                        metric("文件总数", count("file_resources"), "上传资源与内容引用"),
                        metric("待审核文章", countWhere("articles", "status = 'PENDING_REVIEW'"), "文章审核队列"),
                        metric("待审核作品", countWhere("portfolio_projects", "status = 'PENDING_REVIEW'"), "作品审核队列"),
                        metric("待审核评论", countWhere("comments", "status = 'PENDING'"), "评论审核队列"),
                        metric("总访问量", count("visit_logs"), "累计页面访问日志"),
                        metric("总搜索次数", count("search_logs"), "累计搜索日志"),
                        metric("总点赞数", count("like_records"), "累计互动"),
                        metric("总收藏数", count("favorite_records"), "累计收藏")
                ),
                hotArticles(),
                hotProjects(),
                hotSearchKeywords(),
                visitTrend(),
                searchTrend(),
                recentActivities()
        );
    }

    // 读取全表数量。
    private long count(String tableName) {
        Long total = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return total == null ? 0 : total;
    }

    // 读取带条件的表数量。
    private long countWhere(String tableName, String whereClause) {
        Long total = jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + whereClause, Long.class);
        return total == null ? 0 : total;
    }

    // 构造指标卡。
    private MetricVO metric(String label, long value, String note) {
        return new MetricVO(label, String.valueOf(value), note);
    }

    // 热门文章：仅统计公开已发布内容，优先读取聚合统计表。
    private List<ContentRankVO> hotArticles() {
        return cached(CacheKeys.HOT_ARTICLES, new TypeReference<List<ContentRankVO>>() {
        }, HOT_CONTENT_CACHE_TTL, this::queryHotArticles);
    }

    private List<ContentRankVO> queryHotArticles() {
        return jdbcTemplate.query("""
                        with ranked_articles as (
                            select a.id,
                                   a.title,
                                   a.slug,
                                   coalesce(cs.view_count, a.view_count, 0) as view_count,
                                   coalesce(cs.like_count, a.like_count, 0) as like_count,
                                   coalesce(cs.comment_count, a.comment_count, 0) as comment_count,
                                   (
                                       coalesce(cs.view_count, a.view_count, 0)
                                       + coalesce(cs.like_count, a.like_count, 0) * 3
                                       + coalesce(cs.comment_count, a.comment_count, 0) * 4
                                       + case when a.is_recommend then 20 else 0 end
                                       + case when a.is_top then 30 else 0 end
                                   ) as hot_score
                            from articles a
                            left join content_statistics cs
                                   on cs.target_type = 'ARTICLE' and cs.target_id = a.id
                            where a.status = 'PUBLISHED'
                              and a.privacy_type = 'PUBLIC'
                        )
                        select title, slug, view_count, like_count
                        from ranked_articles
                        order by hot_score desc,
                                 view_count desc,
                                 like_count desc,
                                 comment_count desc,
                                 id desc
                        limit 5
                        """,
                (rs, rowNum) -> new ContentRankVO(
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getLong("view_count"),
                        rs.getLong("like_count")
                ));
    }

    // 热门作品：仅统计公开可见作品，优先读取聚合统计表。
    private List<ContentRankVO> hotProjects() {
        return cached(CacheKeys.HOT_PROJECTS, new TypeReference<List<ContentRankVO>>() {
        }, HOT_CONTENT_CACHE_TTL, this::queryHotProjects);
    }

    private List<ContentRankVO> queryHotProjects() {
        return jdbcTemplate.query("""
                        with ranked_projects as (
                            select p.id,
                                   p.title,
                                   p.slug,
                                   coalesce(cs.view_count, 0) as view_count,
                                   coalesce(cs.like_count, 0) as like_count,
                                   coalesce(cs.favorite_count, 0) as favorite_count,
                                   (
                                       coalesce(cs.view_count, 0)
                                       + coalesce(cs.like_count, 0) * 3
                                       + coalesce(cs.favorite_count, 0) * 4
                                       + case when p.is_recommend then 30 else 0 end
                                   ) as hot_score
                            from portfolio_projects p
                            left join content_statistics cs
                                   on cs.target_type = 'PROJECT' and cs.target_id = p.id
                            where p.status = 'VISIBLE'
                        )
                        select title, slug, view_count, like_count
                        from ranked_projects
                        order by hot_score desc,
                                 view_count desc,
                                 like_count desc,
                                 favorite_count desc,
                                 id desc
                        limit 5
                        """,
                (rs, rowNum) -> new ContentRankVO(
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getLong("view_count"),
                        rs.getLong("like_count")
                ));
    }

    // 最近 7 天访问趋势。
    private List<TrendPointVO> visitTrend() {
        return jdbcTemplate.query("""
                        select day::date as metric_date,
                               coalesce(count(v.id), 0) as pv
                        from generate_series(current_date - interval '6 days', current_date, interval '1 day') day
                        left join visit_logs v on v.created_at::date = day::date
                        group by day
                        order by day
                        """,
                (rs, rowNum) -> new TrendPointVO(
                        rs.getObject("metric_date", LocalDate.class),
                        rs.getLong("pv")
                ));
    }

    // 最近 7 天搜索趋势。
    private List<TrendPointVO> searchTrend() {
        return jdbcTemplate.query("""
                        select day::date as metric_date,
                               coalesce(count(s.id), 0) as pv
                        from generate_series(current_date - interval '6 days', current_date, interval '1 day') day
                        left join search_logs s on s.created_at::date = day::date
                        group by day
                        order by day
                        """,
                (rs, rowNum) -> new TrendPointVO(
                        rs.getObject("metric_date", LocalDate.class),
                        rs.getLong("pv")
                ));
    }

    // 热门搜索关键词 top 10。
    private List<SearchKeywordVO> hotSearchKeywords() {
        return cached(CacheKeys.HOT_SEARCH_KEYWORDS, new TypeReference<List<SearchKeywordVO>>() {
        }, HOT_SEARCH_CACHE_TTL, this::queryHotSearchKeywords);
    }

    private List<SearchKeywordVO> queryHotSearchKeywords() {
        return jdbcTemplate.query("""
                        select keyword, count(*) as search_count
                        from search_logs
                        group by keyword
                        order by search_count desc, max(created_at) desc, keyword asc
                        limit 10
                        """,
                (rs, rowNum) -> new SearchKeywordVO(
                        rs.getString("keyword"),
                        rs.getLong("search_count")
                ));
    }

    // 最近后台操作。
    private List<ActivityVO> recentActivities() {
        return jdbcTemplate.query("""
                        select operation, module, to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at
                        from operation_logs
                        order by created_at desc, id desc
                        limit 8
                        """,
                (rs, rowNum) -> new ActivityVO(
                        rs.getString("operation"),
                        rs.getString("module"),
                        rs.getString("created_at")
                ));
    }

    private <T> T cached(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        return redisCacheService.read(key, type).orElseGet(() -> {
            T value = loader.get();
            redisCacheService.write(key, value, ttl);
            return value;
        });
    }

    private <T> T cached(String key, TypeReference<T> typeReference, Duration ttl, Supplier<T> loader) {
        return redisCacheService.read(key, typeReference).orElseGet(() -> {
            T value = loader.get();
            redisCacheService.write(key, value, ttl);
            return value;
        });
    }

    public record DashboardOverviewVO(
            List<MetricVO> metrics,
            List<ContentRankVO> hotArticles,
            List<ContentRankVO> hotProjects,
            List<SearchKeywordVO> hotSearchKeywords,
            List<TrendPointVO> visitTrend,
            List<TrendPointVO> searchTrend,
            List<ActivityVO> recentActivities
    ) {
    }

    public record MetricVO(String label, String value, String note) {
    }

    public record ContentRankVO(String title, String slug, Long views, Long likes) {
    }

    public record TrendPointVO(LocalDate date, Long pv) {
    }

    public record ActivityVO(String operation, String module, String createdAt) {
    }

    public record SearchKeywordVO(String keyword, Long count) {
    }
}
