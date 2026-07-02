package com.creatorspace.module.search;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 公开站内搜索接口，使用 PostgreSQL 原生模糊匹配覆盖公开内容。
 */
@Validated
@RestController
public class SearchController {

    private static final Set<String> SEARCH_TYPES = Set.of("ARTICLE", "PROJECT", "INSPIRATION", "TAG", "CATEGORY", "PAGE");
    private static final Set<String> SORT_TYPES = Set.of("RELEVANCE", "LATEST", "POPULAR");

    private final JdbcTemplate jdbcTemplate;
    private final SearchLogService searchLogService;

    // 通过 JdbcTemplate 组织跨内容类型查询，后续可以替换成专用搜索服务。
    public SearchController(JdbcTemplate jdbcTemplate, SearchLogService searchLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.searchLogService = searchLogService;
    }

    // 搜索公开文章、可见作品、公开灵感、标签、分类和公开页面配置。
    @GetMapping("/api/search")
    public ApiResponse<PageResponse<SearchResultVO>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) long pageSize,
            HttpServletRequest servletRequest,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        String normalizedKeyword = keyword.trim();
        String normalizedType = normalizeType(type);
        String normalizedSort = normalizeSort(sort);
        String pattern = "%" + normalizedKeyword + "%";
        long offset = (page - 1) * pageSize;
        List<SearchResultVO> records = jdbcTemplate.query(searchSql(),
                (rs, rowNum) -> new SearchResultVO(
                        rs.getString("type"),
                        rs.getString("title"),
                        rs.getString("slug"),
                        rs.getString("description"),
                        rs.getString("cover_url"),
                        rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getDouble("score")
                ),
                normalizedKeyword, normalizedKeyword, normalizedKeyword,
                pattern, pattern, pattern, pattern, pattern, pattern, pattern,
                normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword,
                pattern, pattern, pattern, pattern, pattern, pattern,
                normalizedKeyword, normalizedKeyword, normalizedKeyword,
                pattern, pattern, pattern, pattern, pattern,
                normalizedKeyword, normalizedKeyword,
                pattern, pattern, pattern,
                normalizedKeyword, normalizedKeyword, normalizedKeyword,
                pattern, pattern, pattern,
                normalizedKeyword, normalizedKeyword, normalizedKeyword, normalizedKeyword,
                pattern, pattern, pattern, pattern, pattern,
                normalizedType, normalizedType, normalizedSort, normalizedSort,
                pageSize, offset);
        long total = countSearchResults(pattern, normalizedType);
        if (!normalizedKeyword.isEmpty()) {
            searchLogService.record(normalizedKeyword, (int) total, servletRequest, loginUser);
        }
        return ApiResponse.ok(new PageResponse<>(records, page, pageSize, total));
    }

    private String searchSql() {
        return """
                with all_results as (
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
                           ) as score,
                           a.view_count + a.like_count * 3 + a.comment_count * 4 as popularity
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
                           greatest(
                               similarity(p.title, ?),
                               similarity(coalesce(p.description, ''), ?),
                               similarity(coalesce(p.project_type, ''), ?),
                               similarity(coalesce(p.tech_stack::text, ''), ?)
                           ) as score,
                           coalesce(cs.view_count, 0) + coalesce(cs.like_count, 0) * 3 + coalesce(cs.favorite_count, 0) * 4 +
                               case when p.is_recommend then 30 else 0 end as popularity
                    from portfolio_projects p
                    left join content_statistics cs on cs.target_type = 'PROJECT' and cs.target_id = p.id
                    where p.status = 'VISIBLE'
                      and (
                          p.title ilike ?
                          or p.description ilike ?
                          or p.project_type ilike ?
                          or p.tech_stack::text ilike ?
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
                           greatest(similarity(i.title, ?), similarity(coalesce(i.content, ''), ?), similarity(coalesce(i.card_type, ''), ?)) as score,
                           coalesce(cs.view_count, 0) + coalesce(cs.like_count, 0) * 3 + coalesce(cs.favorite_count, 0) * 4 as popularity
                    from inspiration_cards i
                    left join content_statistics cs on cs.target_type = 'INSPIRATION' and cs.target_id = i.id
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
                    union all
                    select 'TAG' as type,
                           t.name as title,
                           t.slug,
                           coalesce('标签权重 ' || t.weight::text, '') as description,
                           null as cover_url,
                           t.created_at as occurred_at,
                           greatest(similarity(t.name, ?), similarity(t.slug, ?)) as score,
                           t.weight as popularity
                    from tags t
                    where t.name ilike ?
                       or t.slug ilike ?
                       or exists (
                           select 1
                           from tag_aliases alias
                           where alias.tag_id = t.id
                             and alias.alias ilike ?
                       )
                    union all
                    select 'CATEGORY' as type,
                           c.name as title,
                           c.slug,
                           c.description,
                           null as cover_url,
                           c.created_at as occurred_at,
                           greatest(similarity(c.name, ?), similarity(c.slug, ?), similarity(coalesce(c.description, ''), ?)) as score,
                           c.sort_order as popularity
                    from categories c
                    where c.enabled = true
                      and (
                          c.name ilike ?
                          or c.slug ilike ?
                          or c.description ilike ?
                      )
                    union all
                    select 'PAGE' as type,
                           p.title,
                           p.slug,
                           coalesce(p.seo_description, p.seo_title, '') as description,
                           null as cover_url,
                           p.updated_at as occurred_at,
                           greatest(
                               similarity(p.title, ?),
                               similarity(p.slug, ?),
                               similarity(coalesce(p.seo_title, ''), ?),
                               similarity(coalesce(p.seo_description, ''), ?)
                           ) as score,
                           0 as popularity
                    from page_configs p
                    where p.status = 'PUBLISHED'
                      and (
                          p.title ilike ?
                          or p.slug ilike ?
                          or p.seo_title ilike ?
                          or p.seo_description ilike ?
                          or p.content_json::text ilike ?
                      )
                )
                select type, title, slug, description, cover_url, occurred_at, score
                from all_results
                where (? = '' or type = ?)
                order by
                    case when ? = 'POPULAR' then popularity end desc nulls last,
                    case when ? = 'LATEST' then occurred_at end desc nulls last,
                    score desc,
                    occurred_at desc nulls last
                limit ? offset ?
                """;
    }


    private String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SEARCH_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("搜索类型不支持");
        }
        return normalized;
    }

    private String normalizeSort(String value) {
        String normalized = value == null || value.isBlank() ? "RELEVANCE" : value.trim().toUpperCase(Locale.ROOT);
        if (!SORT_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("搜索排序不支持");
        }
        return normalized;
    }

    // 统计跨类型搜索结果数量。
    private long countSearchResults(String pattern, String type) {
        Long total = jdbcTemplate.queryForObject("""
                        with result_counts as (
                            select 'ARTICLE' as type,
                                   count(*) as total
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
                                   count(*) as total
                            from portfolio_projects p
                            where p.status = 'VISIBLE'
                              and (
                                  p.title ilike ?
                                  or p.description ilike ?
                                  or p.project_type ilike ?
                                  or p.tech_stack::text ilike ?
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
                                   count(*) as total
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
                            union all
                            select 'TAG' as type,
                                   count(*) as total
                            from tags t
                            where t.name ilike ?
                               or t.slug ilike ?
                               or exists (
                                   select 1
                                   from tag_aliases alias
                                   where alias.tag_id = t.id
                                     and alias.alias ilike ?
                               )
                            union all
                            select 'CATEGORY' as type,
                                   count(*) as total
                            from categories c
                            where c.enabled = true
                              and (
                                  c.name ilike ?
                                  or c.slug ilike ?
                                  or c.description ilike ?
                              )
                            union all
                            select 'PAGE' as type,
                                   count(*) as total
                            from page_configs p
                            where p.status = 'PUBLISHED'
                              and (
                                  p.title ilike ?
                                  or p.slug ilike ?
                                  or p.seo_title ilike ?
                                  or p.seo_description ilike ?
                                  or p.content_json::text ilike ?
                              )
                        )
                        select coalesce(sum(total), 0)
                        from result_counts
                        where (? = '' or type = ?)
                        """,
                Long.class,
                pattern, pattern, pattern, pattern, pattern, pattern, pattern,
                pattern, pattern, pattern, pattern, pattern, pattern,
                pattern, pattern, pattern, pattern, pattern,
                pattern, pattern, pattern,
                pattern, pattern, pattern,
                pattern, pattern, pattern, pattern, pattern,
                type, type);
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
