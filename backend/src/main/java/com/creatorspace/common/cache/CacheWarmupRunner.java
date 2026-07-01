package com.creatorspace.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * 应用启动时预热常用缓存数据。
 */
@Component
public class CacheWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final RedisJsonCacheService redisJsonCacheService;

    public CacheWarmupRunner(JdbcTemplate jdbcTemplate, RedisJsonCacheService redisJsonCacheService) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisJsonCacheService = redisJsonCacheService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始进行 Redis 缓存预热...");
        try {
            warmupPopularArticles();
            warmupPopularProjects();
            warmupPopularInspirations();
            log.info("Redis 缓存预热完成！");
        } catch (Exception e) {
            log.error("Redis 缓存预热失败: ", e);
        }
    }

    private void warmupPopularArticles() {
        log.info("预热热门文章...");
        String sql = """
            select a.id, a.title, a.slug, a.view_count + a.like_count * 3 + a.comment_count * 4 as popularity
            from articles a
            where a.status = 'PUBLISHED' and a.privacy_type = 'PUBLIC'
            order by popularity desc nulls last
            limit 20
            """;
        List<Map<String, Object>> articles = jdbcTemplate.queryForList(sql);
        redisJsonCacheService.write("warmup:popular:articles", articles, Duration.ofSeconds(3600));
    }

    private void warmupPopularProjects() {
        log.info("预热热门作品...");
        String sql = """
            select p.id, p.title, p.slug, 
                   coalesce(cs.view_count, 0) + coalesce(cs.like_count, 0) * 3 + coalesce(cs.favorite_count, 0) * 4 as popularity
            from portfolio_projects p
            left join content_statistics cs on cs.target_type = 'PROJECT' and cs.target_id = p.id
            where p.status = 'VISIBLE'
            order by popularity desc nulls last
            limit 20
            """;
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(sql);
        redisJsonCacheService.write("warmup:popular:projects", projects, Duration.ofSeconds(3600));
    }

    private void warmupPopularInspirations() {
        log.info("预热热门灵感...");
        String sql = """
            select i.id, i.title, 
                   coalesce(cs.view_count, 0) + coalesce(cs.like_count, 0) * 3 + coalesce(cs.favorite_count, 0) * 4 as popularity
            from inspiration_cards i
            left join content_statistics cs on cs.target_type = 'INSPIRATION' and cs.target_id = i.id
            where i.is_public = true
            order by popularity desc nulls last
            limit 20
            """;
        List<Map<String, Object>> inspirations = jdbcTemplate.queryForList(sql);
        redisJsonCacheService.write("warmup:popular:inspirations", inspirations, Duration.ofSeconds(3600));
    }
}
