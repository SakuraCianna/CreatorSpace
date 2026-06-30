package com.creatorspace.module.statistics;

import com.creatorspace.common.cache.CacheKeys;
import com.creatorspace.common.cache.RedisJsonCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DailyMetricsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(DailyMetricsAggregationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RedisJsonCacheService redisCacheService;
    private final ZoneId zoneId;

    public DailyMetricsAggregationService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            RedisJsonCacheService redisCacheService,
            @Value("${app.daily-metrics.zone:Asia/Shanghai}") String zone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.redisCacheService = redisCacheService;
        this.zoneId = ZoneId.of(zone);
    }

    @Scheduled(cron = "${app.daily-metrics.aggregate-cron:0 10 1 * * *}", zone = "${app.daily-metrics.zone:Asia/Shanghai}")
    public void aggregateYesterday() {
        LocalDate targetDate = LocalDate.now(zoneId).minusDays(1);
        try {
            aggregate(targetDate);
        } catch (RuntimeException exception) {
            log.warn("Daily metrics aggregation failed for {}", targetDate, exception);
        }
    }

    public AggregationResult aggregate(LocalDate metricDate) {
        List<DailyMetricVO> metrics = new ArrayList<>();
        metrics.add(upsertCount(metricDate, DailyMetricKeys.SITE_PV, "visit_logs", "访问 PV", "select count(*) from visit_logs where created_at::date = ?"));
        metrics.add(upsertCount(metricDate, DailyMetricKeys.SITE_UV, "visit_logs", "访问 UV", "select count(distinct ip_address) from visit_logs where created_at::date = ?"));
        metrics.add(upsertCount(metricDate, DailyMetricKeys.SEARCH_COUNT, "search_logs", "搜索次数", "select count(*) from search_logs where created_at::date = ?"));
        metrics.add(upsertCount(metricDate, DailyMetricKeys.COMMENT_COUNT, "comments", "评论数", "select count(*) from comments where created_at::date = ?"));
        metrics.add(upsertCount(metricDate, DailyMetricKeys.LIKE_COUNT, "like_records", "点赞数", "select count(*) from like_records where created_at::date = ?"));
        metrics.add(upsertCount(metricDate, DailyMetricKeys.FAVORITE_COUNT, "favorite_records", "收藏数", "select count(*) from favorite_records where created_at::date = ?"));
        metrics.add(upsertCount(metricDate, DailyMetricKeys.OPERATION_COUNT, "operation_logs", "后台操作数", "select count(*) from operation_logs where created_at::date = ?"));
        metrics.add(upsertSnapshot(metricDate, DailyMetricKeys.HOT_ARTICLES, queryHotArticlesSnapshot()));
        metrics.add(upsertSnapshot(metricDate, DailyMetricKeys.HOT_PROJECTS, queryHotProjectsSnapshot()));
        redisCacheService.evict(CacheKeys.DASHBOARD_OVERVIEW);
        return new AggregationResult(metricDate, metrics);
    }

    public List<DailyMetricVO> list(LocalDate startDate, LocalDate endDate, String metricKey) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("where metric_date between ? and ?");
        params.add(startDate);
        params.add(endDate);
        if (metricKey != null && !metricKey.isBlank()) {
            where.append(" and metric_key = ?");
            params.add(metricKey.trim());
        }
        return jdbcTemplate.query("""
                        select metric_date, metric_key, metric_value, detail_json::text as detail_json, updated_at
                        from daily_metrics
                        %s
                        order by metric_date desc, metric_key asc
                        limit 500
                        """.formatted(where),
                (rs, rowNum) -> toMetric(rs),
                params.toArray());
    }

    private DailyMetricVO upsertCount(LocalDate metricDate, String metricKey, String source, String label, String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, metricDate);
        long metricValue = value == null ? 0 : value;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("label", label);
        detail.put("source", source);
        detail.put("date", metricDate.toString());
        return upsert(metricDate, metricKey, metricValue, detail);
    }

    private DailyMetricVO upsertSnapshot(LocalDate metricDate, String metricKey, List<HotContentSnapshot> records) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("date", metricDate.toString());
        detail.put("records", records);
        return upsert(metricDate, metricKey, records.size(), detail);
    }

    private DailyMetricVO upsert(LocalDate metricDate, String metricKey, long metricValue, Object detail) {
        String detailJson = toJson(detail);
        jdbcTemplate.update("""
                        insert into daily_metrics (metric_date, metric_key, metric_value, detail_json, updated_at)
                        values (?, ?, ?, cast(? as jsonb), now())
                        on conflict (metric_date, metric_key)
                        do update set metric_value = excluded.metric_value,
                                      detail_json = excluded.detail_json,
                                      updated_at = now()
                        """,
                metricDate, metricKey, metricValue, detailJson);
        return new DailyMetricVO(metricDate, metricKey, metricValue, detailJson, null);
    }

    private List<HotContentSnapshot> queryHotArticlesSnapshot() {
        return jdbcTemplate.query("""
                        with ranked_articles as (
                            select a.id,
                                   a.title,
                                   a.slug,
                                   coalesce(cs.view_count, a.view_count, 0) as view_count,
                                   coalesce(cs.like_count, a.like_count, 0) as like_count,
                                   coalesce(cs.comment_count, a.comment_count, 0) as comment_count,
                                   0::bigint as favorite_count,
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
                        select 'ARTICLE' as target_type, id as target_id, title, slug, view_count, like_count, comment_count, favorite_count, hot_score
                        from ranked_articles
                        order by hot_score desc, view_count desc, like_count desc, comment_count desc, id desc
                        limit 5
                        """,
                (rs, rowNum) -> toHotContent(rs));
    }

    private List<HotContentSnapshot> queryHotProjectsSnapshot() {
        return jdbcTemplate.query("""
                        with ranked_projects as (
                            select p.id,
                                   p.title,
                                   p.slug,
                                   coalesce(cs.view_count, 0) as view_count,
                                   coalesce(cs.like_count, 0) as like_count,
                                   coalesce(cs.comment_count, 0) as comment_count,
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
                        select 'PROJECT' as target_type, id as target_id, title, slug, view_count, like_count, comment_count, favorite_count, hot_score
                        from ranked_projects
                        order by hot_score desc, view_count desc, like_count desc, favorite_count desc, id desc
                        limit 5
                        """,
                (rs, rowNum) -> toHotContent(rs));
    }

    private HotContentSnapshot toHotContent(ResultSet rs) throws SQLException {
        return new HotContentSnapshot(
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getLong("view_count"),
                rs.getLong("like_count"),
                rs.getLong("comment_count"),
                rs.getLong("favorite_count"),
                rs.getLong("hot_score")
        );
    }

    private DailyMetricVO toMetric(ResultSet rs) throws SQLException {
        return new DailyMetricVO(
                rs.getObject("metric_date", LocalDate.class),
                rs.getString("metric_key"),
                rs.getLong("metric_value"),
                rs.getString("detail_json"),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private String toJson(Object detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? Map.of() : detail);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public record AggregationResult(LocalDate metricDate, List<DailyMetricVO> metrics) {
    }

    public record DailyMetricVO(LocalDate metricDate, String metricKey, Long metricValue, String detailJson, OffsetDateTime updatedAt) {
    }

    public record HotContentSnapshot(
            String targetType,
            Long targetId,
            String title,
            String slug,
            Long views,
            Long likes,
            Long comments,
            Long favorites,
            Long score
    ) {
    }
}