package com.creatorspace.module.search;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 公开站内搜索接口，第一阶段用 PostgreSQL 原生模糊匹配覆盖核心内容。
 */
@Validated
@RestController
public class SearchController {

    private final JdbcTemplate jdbcTemplate;

    // 通过 JdbcTemplate 组织跨内容类型查询，后续可以替换成专用搜索服务。
    public SearchController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 搜索公开文章、可见作品和公开灵感卡片。
    @GetMapping("/api/search")
    public ApiResponse<PageResponse<SearchResultVO>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) long pageSize,
            HttpServletRequest servletRequest
    ) {
        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return ApiResponse.ok(new PageResponse<>(List.of(), page, pageSize, 0));
        }
        String searchTerm = normalizedKeyword;
        String pattern = "%" + normalizedKeyword + "%";
        long offset = (page - 1) * pageSize;
        List<SearchResultVO> records = jdbcTemplate.query("""
                        select *
                        from (
                            select 'ARTICLE' as type,
                                   a.title,
                                   a.slug,
                                   a.summary as description,
                                   a.cover_url as cover_url,
                                   a.publish_time as occurred_at,
                                   greatest(
                                       similarity(a.title, ?),
                                       similarity(coalesce(a.summary, ''), ?),
                                       similarity(coalesce(c.name, ''), ?)
                                   ) as score
                            from articles a
                            left join categories c on c.id = a.category_id
                            where a.status = 'PUBLISHED'
                              and a.privacy_type = 'PUBLIC'
                              and (
                                  a.title ilike ?
                                  or a.summary ilike ?
                                  or a.content_markdown ilike ?
                                  or c.name ilike ?
                                  or c.slug ilike ?
                                  or exists (
                                      select 1
                                      from article_tags article_tag
                                      join tags tag on tag.id = article_tag.tag_id
                                      where article_tag.article_id = a.id
                                        and (tag.name ilike ? or tag.slug ilike ?)
                                  )
                              )
                            union all
                            select 'PROJECT' as type,
                                   p.title,
                                   p.slug,
                                   p.description,
                                   p.cover_url,
                                   p.created_at as occurred_at,
                                   greatest(similarity(p.title, ?), similarity(coalesce(p.description, ''), ?)) as score
                            from portfolio_projects p
                            where p.status = 'VISIBLE'
                              and (
                                  p.title ilike ?
                                  or p.description ilike ?
                                  or p.project_type ilike ?
                                  or exists (
                                      select 1
                                      from project_tags project_tag
                                      join tags tag on tag.id = project_tag.tag_id
                                      where project_tag.project_id = p.id
                                        and (tag.name ilike ? or tag.slug ilike ?)
                                  )
                              )
                            union all
                            select 'INSPIRATION' as type,
                                   i.title,
                                   i.id::text as slug,
                                   i.content as description,
                                   i.image_url as cover_url,
                                   i.created_at as occurred_at,
                                   greatest(similarity(i.title, ?), similarity(coalesce(i.content, ''), ?)) as score
                            from inspiration_cards i
                            where i.is_public = true
                              and (
                                  i.title ilike ?
                                  or i.content ilike ?
                                  or i.card_type ilike ?
                                  or exists (
                                      select 1
                                      from inspiration_tags inspiration_tag
                                      join tags tag on tag.id = inspiration_tag.tag_id
                                      where inspiration_tag.inspiration_id = i.id
                                        and (tag.name ilike ? or tag.slug ilike ?)
                                  )
                              )
                        ) results
                        order by score desc, occurred_at desc nulls last
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new SearchResultVO(
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getString("description"),
                        rs.getString("cover_url"),
                        rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getDouble("score")
                ),
                searchTerm, searchTerm, searchTerm,
                pattern, pattern, pattern, pattern, pattern, pattern, pattern,
                searchTerm, searchTerm,
                pattern, pattern, pattern, pattern, pattern,
                searchTerm, searchTerm,
                pattern, pattern, pattern, pattern, pattern,
                pageSize, offset);
        long total = countSearchResults(pattern);
        recordSearchLog(normalizedKeyword, (int) total, servletRequest);
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total));
    }

    // 记录搜索日志，用于后台热门搜索关键词分析。
    private void recordSearchLog(String keyword, int resultCount, HttpServletRequest request) {
        try {
            jdbcTemplate.update("""
                    insert into search_logs (keyword, result_count, ip_address, user_agent)
                    values (?, ?, cast(? as inet), ?)
                    """,
                    keyword,
                    resultCount,
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent"));
        } catch (Exception ignored) {
            // 搜索日志记录失败不应影响搜索结果返回
        }
    }

    // 统计跨类型搜索结果数量。
    private long countSearchResults(String pattern) {
        Long total = jdbcTemplate.queryForObject("""
                        select (
                            select count(*)
                            from articles a
                            left join categories c on c.id = a.category_id
                            where a.status = 'PUBLISHED'
                              and a.privacy_type = 'PUBLIC'
                              and (
                                  a.title ilike ?
                                  or a.summary ilike ?
                                  or a.content_markdown ilike ?
                                  or c.name ilike ?
                                  or c.slug ilike ?
                                  or exists (
                                      select 1
                                      from article_tags article_tag
                                      join tags tag on tag.id = article_tag.tag_id
                                      where article_tag.article_id = a.id
                                        and (tag.name ilike ? or tag.slug ilike ?)
                                  )
                              )
                        ) + (
                            select count(*)
                            from portfolio_projects p
                            where p.status = 'VISIBLE'
                              and (
                                  p.title ilike ?
                                  or p.description ilike ?
                                  or p.project_type ilike ?
                                  or exists (
                                      select 1
                                      from project_tags project_tag
                                      join tags tag on tag.id = project_tag.tag_id
                                      where project_tag.project_id = p.id
                                        and (tag.name ilike ? or tag.slug ilike ?)
                                  )
                              )
                        ) + (
                            select count(*)
                            from inspiration_cards i
                            where i.is_public = true
                              and (
                                  i.title ilike ?
                                  or i.content ilike ?
                                  or i.card_type ilike ?
                                  or exists (
                                      select 1
                                      from inspiration_tags inspiration_tag
                                      join tags tag on tag.id = inspiration_tag.tag_id
                                      where inspiration_tag.inspiration_id = i.id
                                        and (tag.name ilike ? or tag.slug ilike ?)
                                  )
                              )
                        )
                        """,
                Long.class,
                pattern, pattern, pattern, pattern, pattern, pattern, pattern,
                pattern, pattern, pattern, pattern, pattern,
                pattern, pattern, pattern, pattern, pattern);
        return total == null ? 0 : total;
    }

    public record SearchResultVO(
            String type,
            String title,
            String slug,
            String description,
            String coverUrl,
            OffsetDateTime occurredAt,
            Double score
    ) {
    }
}
