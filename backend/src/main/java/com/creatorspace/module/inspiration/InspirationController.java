package com.creatorspace.module.inspiration;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.tag.vo.TagVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开灵感墙接口，展示摘句、Prompt、链接、代码片段和视觉参考。
 */
@Validated
@RestController
public class InspirationController {

    private final JdbcTemplate jdbcTemplate;

    // 通过 JdbcTemplate 读取灵感卡片和标签，保持接口轻量稳定。
    public InspirationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 查询公开灵感卡片列表。
    @GetMapping("/api/inspirations")
    public ApiResponse<PageResponse<InspirationVO>> listPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) long pageSize
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        long offset = (page - 1) * pageSize;
        List<Object> params = new java.util.ArrayList<>();
        String where = buildWhere(normalizedKeyword, normalizedType, params);
        long total = jdbcTemplate.queryForObject("select count(*) from inspiration_cards " + where,
                Long.class,
                params.toArray());

        List<Object> listParams = new java.util.ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<InspirationVO> records = jdbcTemplate.query("""
                        select id,
                               title,
                               content,
                               image_url,
                               card_type,
                               source_url,
                               color,
                               sort_order,
                               created_at
                        from inspiration_cards
                        %s
                        order by sort_order asc, created_at desc, id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toInspiration(rs),
                listParams.toArray());

        Map<Long, List<TagVO>> tagsByCard = tagsByCard(records.stream().map(InspirationVO::id).toList());
        List<InspirationVO> withTags = records.stream()
                .map(card -> card.withTags(tagsByCard.getOrDefault(card.id(), Collections.emptyList())))
                .toList();
        return ApiResponse.ok(new PageResponse<>(withTags, page, pageSize, total));
    }

    // 构造灵感墙查询条件。
    private String buildWhere(String keyword, String type, List<Object> params) {
        StringBuilder where = new StringBuilder("where is_public = true");
        if (!keyword.isEmpty()) {
            where.append(" and (title ilike ? or content ilike ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (!type.isEmpty()) {
            where.append(" and card_type = ?");
            params.add(type);
        }
        return where.toString();
    }

    // 查询指定灵感卡片的标签。
    private Map<Long, List<TagVO>> tagsByCard(List<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(cardIds.size(), "?"));
        Map<Long, List<TagVO>> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        select it.inspiration_id,
                               t.id,
                               t.name,
                               t.slug,
                               t.color,
                               t.weight
                        from inspiration_tags it
                        join tags t on t.id = it.tag_id
                        where it.inspiration_id in (%s)
                        order by t.weight desc, t.name asc
                        """.formatted(placeholders),
                rs -> {
                    Long inspirationId = rs.getLong("inspiration_id");
                    result.computeIfAbsent(inspirationId, key -> new java.util.ArrayList<>())
                            .add(new TagVO(
                                    rs.getLong("id"),
                                    rs.getString("name"),
                                    rs.getString("slug"),
                                    rs.getString("color"),
                                    rs.getInt("weight")
                            ));
                },
                cardIds.toArray());
        return result;
    }

    // 将结果集转换成公开灵感视图对象。
    private InspirationVO toInspiration(ResultSet rs) throws SQLException {
        return new InspirationVO(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("image_url"),
                rs.getString("card_type"),
                rs.getString("source_url"),
                rs.getString("color"),
                rs.getInt("sort_order"),
                rs.getObject("created_at", OffsetDateTime.class),
                Collections.emptyList()
        );
    }

    public record InspirationVO(
            Long id,
            String title,
            String content,
            String imageUrl,
            String cardType,
            String sourceUrl,
            String color,
            Integer sortOrder,
            OffsetDateTime createdAt,
            List<TagVO> tags
    ) {
        InspirationVO withTags(List<TagVO> tags) {
            return new InspirationVO(id, title, content, imageUrl, cardType, sourceUrl, color, sortOrder, createdAt, tags);
        }
    }
}
