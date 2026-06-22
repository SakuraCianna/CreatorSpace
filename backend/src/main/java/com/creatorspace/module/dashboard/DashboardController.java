package com.creatorspace.module.dashboard;

import com.creatorspace.common.result.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 后台运营概览接口，提供 CMS 首页需要的内容、互动和访问摘要。
 */
@RestController
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    // 通过聚合 SQL 生成后台概览，避免后台页面继续依赖硬编码样例。
    public DashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 返回后台仪表盘概览数据。
    @GetMapping("/api/admin/dashboard/overview")
    public ApiResponse<DashboardOverviewVO> overview() {
        return ApiResponse.ok(new DashboardOverviewVO(
                List.of(
                        metric("文章", count("articles"), "公开/私密/草稿统一管理"),
                        metric("作品", count("portfolio_projects"), "作品档案、截图和外链"),
                        metric("灵感", count("inspiration_cards"), "摘句、Prompt、链接和参考图"),
                        metric("评论", count("comments"), "全部评论记录"),
                        metric("待审核", countWhere("comments", "status = 'PENDING'"), "评论审核队列"),
                        metric("点赞", count("like_records"), "累计互动"),
                        metric("收藏", count("favorite_records"), "累计收藏"),
                        metric("搜索", count("search_logs"), "累计搜索次数")
                ),
                hotArticles(),
                hotProjects(),
                hotSearchKeywords(),
                visitTrend(),
                recentActivities()
        ));
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

    // 热门文章。
    private List<ContentRankVO> hotArticles() {
        return jdbcTemplate.query("""
                        select title, slug, view_count, like_count
                        from articles
                        order by view_count desc, like_count desc, id desc
                        limit 5
                        """,
                (rs, rowNum) -> new ContentRankVO(
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getLong("view_count"),
                        rs.getLong("like_count")
                ));
    }

    // 热门作品。
    private List<ContentRankVO> hotProjects() {
        return jdbcTemplate.query("""
                        select title, slug, sort_order as view_count, 0 as like_count
                        from portfolio_projects
                        order by is_recommend desc, sort_order asc, id desc
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

    // 热门搜索关键词 top 10。
    private List<SearchKeywordVO> hotSearchKeywords() {
        return jdbcTemplate.query("""
                        select keyword, count(*) as search_count
                        from search_logs
                        group by keyword
                        order by search_count desc
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
                        select operation, module, created_at::text as created_at
                        from operation_logs
                        order by created_at desc
                        limit 8
                        """,
                (rs, rowNum) -> new ActivityVO(
                        rs.getString("operation"),
                        rs.getString("module"),
                        rs.getString("created_at")
                ));
    }

    public record DashboardOverviewVO(
            List<MetricVO> metrics,
            List<ContentRankVO> hotArticles,
            List<ContentRankVO> hotProjects,
            List<SearchKeywordVO> hotSearchKeywords,
            List<TrendPointVO> visitTrend,
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
