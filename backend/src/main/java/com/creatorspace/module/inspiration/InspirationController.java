package com.creatorspace.module.inspiration;

import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.tag.vo.TagVO;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        String where = buildWhere(true, normalizedKeyword, normalizedType, params);
        long total = countCards(where, params);

        List<Object> listParams = new java.util.ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<InspirationVO> records = listCards(where, listParams);

        List<Long> cardIds = records.stream().map(InspirationVO::id).toList();
        Map<Long, List<TagVO>> tagsByCard = tagsByCard(cardIds);
        Map<Long, List<InspirationRelationVO>> relationsByCard = relationsByCard(cardIds, true);
        List<InspirationVO> withTags = records.stream()
                .map(card -> card.withDetails(
                        tagsByCard.getOrDefault(card.id(), Collections.emptyList()),
                        relationsByCard.getOrDefault(card.id(), Collections.emptyList())
                ))
                .toList();
        return ApiResponse.ok(new PageResponse<>(withTags, page, pageSize, total));
    }

    // 管理员查询全部灵感卡片，包括私密卡片。
    @GetMapping("/api/admin/inspirations")
    public ApiResponse<PageResponse<InspirationVO>> listAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) long pageSize
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        long offset = (page - 1) * pageSize;
        List<Object> params = new java.util.ArrayList<>();
        String where = buildWhere(false, normalizedKeyword, normalizedType, params);
        long total = countCards(where, params);

        List<Object> listParams = new java.util.ArrayList<>(params);
        listParams.add(pageSize);
        listParams.add(offset);
        List<InspirationVO> records = listCards(where, listParams);
        List<Long> cardIds = records.stream().map(InspirationVO::id).toList();
        Map<Long, List<TagVO>> tagsByCard = tagsByCard(cardIds);
        Map<Long, List<InspirationRelationVO>> relationsByCard = relationsByCard(cardIds, false);
        return ApiResponse.ok(new PageResponse<>(
                records.stream()
                        .map(card -> card.withDetails(
                                tagsByCard.getOrDefault(card.id(), Collections.emptyList()),
                                relationsByCard.getOrDefault(card.id(), Collections.emptyList())
                        ))
                        .toList(),
                page,
                pageSize,
                total
        ));
    }

    // 管理员创建灵感卡片。
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/api/admin/inspirations")
    public ApiResponse<InspirationVO> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody InspirationRequest request
    ) {
        String cardType = normalizeCardType(request.cardType());
        validateTagIds(request.tagIds());
        String imageUrl = normalizeOptionalUrl(request.imageUrl(), "图片地址", true);
        String sourceUrl = normalizeOptionalUrl(request.sourceUrl(), "来源链接", false);
        Long id = jdbcTemplate.queryForObject("""
                        insert into inspiration_cards (
                            title,
                            content,
                            image_url,
                            card_type,
                            source_url,
                            color,
                            is_public,
                            sort_order,
                            created_by
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                request.title().trim(),
                blankToNull(request.content()),
                imageUrl,
                cardType,
                sourceUrl,
                blankToNull(request.color()),
                request.isPublic() == null || request.isPublic(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                loginUser.userId());
        replaceTags(id, request.tagIds());
        return ApiResponse.ok(getCard(id));
    }

    // 管理员更新灵感卡片。
    @Transactional(rollbackFor = Exception.class)
    @PutMapping("/api/admin/inspirations/{id}")
    public ApiResponse<InspirationVO> update(
            @PathVariable Long id,
            @Valid @RequestBody InspirationRequest request
    ) {
        String cardType = normalizeCardType(request.cardType());
        validateTagIds(request.tagIds());
        int affected = jdbcTemplate.update("""
                        update inspiration_cards
                        set title = ?,
                            content = ?,
                            image_url = ?,
                            card_type = ?,
                            source_url = ?,
                            color = ?,
                            is_public = ?,
                            sort_order = ?,
                            updated_at = now()
                        where id = ?
                        """,
                request.title().trim(),
                blankToNull(request.content()),
                normalizeOptionalUrl(request.imageUrl(), "图片地址", true),
                cardType,
                normalizeOptionalUrl(request.sourceUrl(), "来源链接", false),
                blankToNull(request.color()),
                request.isPublic() == null || request.isPublic(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                id);
        if (affected == 0) {
            throw BusinessException.notFound("灵感卡片不存在");
        }
        replaceTags(id, request.tagIds());
        return ApiResponse.ok(getCard(id));
    }

    // 管理员删除灵感卡片。
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/api/admin/inspirations/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        int affected = jdbcTemplate.update("delete from inspiration_cards where id = ?", id);
        if (affected == 0) {
            throw BusinessException.notFound("灵感卡片不存在");
        }
        return ApiResponse.ok(null);
    }

    // 构造灵感墙查询条件。
    private String buildWhere(boolean publicOnly, String keyword, String type, List<Object> params) {
        StringBuilder where = new StringBuilder(publicOnly ? "where is_public = true" : "where 1 = 1");
        if (!keyword.isEmpty()) {
            where.append("""
                     and (
                        title ilike ?
                        or content ilike ?
                        or exists (
                            select 1
                            from inspiration_tags it
                            join tags t on t.id = it.tag_id
                            where it.inspiration_id = inspiration_cards.id
                              and (t.name ilike ? or t.slug ilike ?)
                        )
                    )
                    """);
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
        }
        if (!type.isEmpty()) {
            where.append(" and card_type = ?");
            params.add(type);
        }
        return where.toString();
    }

    // 查询灵感卡片数量。
    private long countCards(String where, List<Object> params) {
        Long total = jdbcTemplate.queryForObject("select count(*) from inspiration_cards " + where,
                Long.class,
                params.toArray());
        return total == null ? 0 : total;
    }

    // 查询灵感卡片列表。
    private List<InspirationVO> listCards(String where, List<Object> params) {
        return jdbcTemplate.query("""
                        select id,
                               title,
                               content,
                               image_url,
                               card_type,
                               source_url,
                               color,
                               is_public,
                               sort_order,
                               created_at
                        from inspiration_cards
                        %s
                        order by sort_order asc, created_at desc, id desc
                        limit ? offset ?
                        """.formatted(where),
                (rs, rowNum) -> toInspiration(rs),
                params.toArray());
    }

    // 读取单张灵感卡片。
    private InspirationVO getCard(Long id) {
        InspirationVO card = jdbcTemplate.query("""
                        select id,
                               title,
                               content,
                               image_url,
                               card_type,
                               source_url,
                               color,
                               is_public,
                               sort_order,
                               created_at
                        from inspiration_cards
                        where id = ?
                        """,
                (rs, rowNum) -> toInspiration(rs),
                id).stream().findFirst().orElseThrow(() -> BusinessException.notFound("灵感卡片不存在"));
        return card.withDetails(
                tagsByCard(List.of(id)).getOrDefault(id, Collections.emptyList()),
                relationsByCard(List.of(id), false).getOrDefault(id, Collections.emptyList())
        );
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

    // 查询灵感卡片关联的文章、作品或其他公开灵感。
    private Map<Long, List<InspirationRelationVO>> relationsByCard(List<Long> cardIds, boolean publicOnly) {
        if (cardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(cardIds.size(), "?"));
        String articleVisibility = publicOnly ? "and a.status = 'PUBLISHED' and a.privacy_type = 'PUBLIC'" : "";
        String projectVisibility = publicOnly ? "and p.status = 'VISIBLE'" : "";
        String inspirationVisibility = publicOnly ? "and target_inspiration.is_public = true" : "";
        String targetVisibility = publicOnly
                ? """
                   and (
                       (ir.target_type = 'ARTICLE' and a.id is not null)
                       or (ir.target_type = 'PROJECT' and p.id is not null)
                       or (ir.target_type = 'INSPIRATION' and target_inspiration.id is not null)
                   )
                  """
                : "";
        Map<Long, List<InspirationRelationVO>> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        select ir.inspiration_id,
                               ir.target_type,
                               ir.target_id,
                               ir.relation_type,
                               case
                                   when ir.target_type = 'ARTICLE' then a.title
                                   when ir.target_type = 'PROJECT' then p.title
                                   when ir.target_type = 'INSPIRATION' then target_inspiration.title
                               end as target_title,
                               case
                                   when ir.target_type = 'ARTICLE' then a.slug
                                   when ir.target_type = 'PROJECT' then p.slug
                                   when ir.target_type = 'INSPIRATION' then null
                               end as target_slug
                        from inspiration_relations ir
                        left join articles a
                               on ir.target_type = 'ARTICLE'
                              and a.id = ir.target_id
                              %s
                        left join portfolio_projects p
                               on ir.target_type = 'PROJECT'
                              and p.id = ir.target_id
                              %s
                        left join inspiration_cards target_inspiration
                               on ir.target_type = 'INSPIRATION'
                              and target_inspiration.id = ir.target_id
                              %s
                        where ir.inspiration_id in (%s)
                        %s
                        order by ir.created_at desc, ir.id desc
                        """.formatted(articleVisibility, projectVisibility, inspirationVisibility, placeholders, targetVisibility),
                rs -> {
                    Long inspirationId = rs.getLong("inspiration_id");
                    result.computeIfAbsent(inspirationId, key -> new java.util.ArrayList<>())
                            .add(new InspirationRelationVO(
                                    rs.getString("target_type"),
                                    rs.getLong("target_id"),
                                    rs.getString("relation_type"),
                                    rs.getString("target_title"),
                                    rs.getString("target_slug")
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
                rs.getBoolean("is_public"),
                rs.getInt("sort_order"),
                rs.getObject("created_at", OffsetDateTime.class),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    // 替换灵感卡片标签。
    private void replaceTags(Long inspirationId, List<Long> tagIds) {
        jdbcTemplate.update("delete from inspiration_tags where inspiration_id = ?", inspirationId);
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            jdbcTemplate.update("""
                            insert into inspiration_tags (inspiration_id, tag_id)
                            values (?, ?)
                            on conflict do nothing
                            """,
                    inspirationId,
                    tagId);
        }
    }

    // 校验标签集合是否都存在。
    private void validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        if (tagIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw BusinessException.badRequest("标签不存在");
        }
        String placeholders = String.join(",", Collections.nCopies(tagIds.size(), "?"));
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from tags where id in (" + placeholders + ")",
                Long.class,
                tagIds.toArray());
        if (count == null || count != tagIds.stream().distinct().count()) {
            throw BusinessException.badRequest("标签不存在");
        }
    }

    // 规范化灵感卡片类型。
    private String normalizeCardType(String value) {
        String cardType = value.trim().toUpperCase();
        if (!Set.of("IMAGE", "TEXT", "PROMPT", "CODE", "LINK", "SKETCH", "REFERENCE").contains(cardType)) {
            throw BusinessException.badRequest("灵感类型不合法");
        }
        return cardType;
    }

    // 外链只允许 http/https；图片允许引用站内上传资源。
    private String normalizeOptionalUrl(String value, String fieldName, boolean allowUploadedResource) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        if (allowUploadedResource && normalized.startsWith("/uploads/")) {
            return normalized;
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return normalized;
            }
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest(fieldName + "格式不合法");
        }
        throw BusinessException.badRequest(fieldName + "只允许 http 或 https 地址");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record InspirationVO(
            Long id,
            String title,
            String content,
            String imageUrl,
            String cardType,
            String sourceUrl,
            String color,
            Boolean isPublic,
            Integer sortOrder,
            OffsetDateTime createdAt,
            List<TagVO> tags,
            List<InspirationRelationVO> relations
    ) {
        InspirationVO withDetails(List<TagVO> tags, List<InspirationRelationVO> relations) {
            return new InspirationVO(id, title, content, imageUrl, cardType, sourceUrl, color, isPublic, sortOrder, createdAt, tags, relations);
        }
    }

    public record InspirationRelationVO(
            String targetType,
            Long targetId,
            String relationType,
            String targetTitle,
            String targetSlug
    ) {
    }

    public record InspirationRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String content,
            String imageUrl,
            @NotBlank String cardType,
            String sourceUrl,
            @Size(max = 32) String color,
            Boolean isPublic,
            Integer sortOrder,
            List<Long> tagIds
    ) {
    }
}
