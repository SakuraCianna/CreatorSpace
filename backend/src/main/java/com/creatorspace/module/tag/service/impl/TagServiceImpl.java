package com.creatorspace.module.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.entity.TagEntity;
import com.creatorspace.module.tag.mapper.TagMapper;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TagServiceImpl implements TagService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$");
    private static final RowMapper<TagVO> TAG_ROW_MAPPER = (rs, rowNum) -> new TagVO(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("slug"),
            rs.getString("color"),
            rs.getInt("weight")
    );

    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;

    public TagServiceImpl(TagMapper tagMapper, JdbcTemplate jdbcTemplate) {
        this.tagMapper = tagMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO create(TagRequest request) {
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(null, slug);

        TagEntity tag = new TagEntity();
        tag.setName(cleanRequired(request.name(), "标签名称不能为空"));
        tag.setSlug(slug);
        tag.setColor(normalizeColor(request.color()));
        tag.setWeight(normalizeWeight(request.weight()));
        tagMapper.insert(tag);
        return toVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO update(Long id, TagRequest request) {
        TagEntity tag = requireTag(id);
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(id, slug);

        tag.setName(cleanRequired(request.name(), "标签名称不能为空"));
        tag.setSlug(slug);
        tag.setColor(normalizeColor(request.color()));
        tag.setWeight(normalizeWeight(request.weight()));
        tagMapper.updateById(tag);
        return toVO(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTag(id);
        long referenceCount = referenceCount(id);
        if (referenceCount > 0) {
            throw BusinessException.conflict("标签已被内容引用，不能删除");
        }
        tagMapper.deleteById(id);
    }

    @Override
    public List<TagVO> list() {
        return tagMapper.selectList(new LambdaQueryWrapper<TagEntity>()
                        .orderByDesc(TagEntity::getWeight)
                        .orderByAsc(TagEntity::getName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<TagVO> recommend(Long userId, String ipAddress, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 60));
        if (userId != null) {
            List<TagVO> personalized = recommendedByUser(userId, boundedLimit);
            if (!personalized.isEmpty()) {
                return personalized;
            }
        }
        return recommendedForVisitor(ipAddress, boundedLimit);
    }

    @Override
    public List<TagVO> listByIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectList(new LambdaQueryWrapper<TagEntity>().in(TagEntity::getId, tagIds))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private List<TagVO> recommendedByUser(Long userId, int limit) {
        return jdbcTemplate.query("""
                        select t.id, t.name, t.slug, t.color, t.weight
                        from tags t
                        left join (
                            select at.tag_id,
                                   sum(24.0 / (1.0 + extract(epoch from (now() - vl.created_at)) / 86400.0 / 7.0)) as preference_score
                            from visit_logs vl
                            join article_tags at on at.article_id = vl.target_id
                            where vl.target_type = 'ARTICLE'
                              and vl.user_id = ?
                              and vl.created_at >= now() - interval '30 days'
                            group by at.tag_id
                        ) preference on preference.tag_id = t.id
                        left join (
                            select tag_id, count(*) as content_count
                            from (
                                select tag_id from article_tags
                                union all
                                select tag_id from project_tags
                                union all
                                select tag_id from inspiration_tags
                            ) usage_rows
                            group by tag_id
                        ) usage on usage.tag_id = t.id
                        order by
                            coalesce(preference.preference_score, 0) desc,
                            (t.weight + coalesce(usage.content_count, 0) * 4 + random() * 6) desc,
                            t.name asc
                        limit ?
                        """,
                TAG_ROW_MAPPER,
                userId,
                limit
        );
    }

    private List<TagVO> recommendedForVisitor(String ipAddress, int limit) {
        return jdbcTemplate.query("""
                        select t.id, t.name, t.slug, t.color, t.weight
                        from tags t
                        left join (
                            select at.tag_id,
                                   sum(12.0 / (1.0 + extract(epoch from (now() - vl.created_at)) / 86400.0 / 7.0)) as visitor_score
                            from visit_logs vl
                            join article_tags at on at.article_id = vl.target_id
                            where vl.target_type = 'ARTICLE'
                              and vl.ip_address = cast(? as inet)
                              and vl.created_at >= now() - interval '7 days'
                            group by at.tag_id
                        ) visitor on visitor.tag_id = t.id
                        left join (
                            select tag_id, count(*) as content_count
                            from (
                                select tag_id from article_tags
                                union all
                                select tag_id from project_tags
                                union all
                                select tag_id from inspiration_tags
                            ) usage_rows
                            group by tag_id
                        ) usage on usage.tag_id = t.id
                        order by
                            (coalesce(visitor.visitor_score, 0) + t.weight + coalesce(usage.content_count, 0) * 5 + random() * 18) desc,
                            t.name asc
                        limit ?
                        """,
                TAG_ROW_MAPPER,
                ipAddress,
                limit
        );
    }

    private void ensureSlugAvailable(Long currentId, String slug) {
        LambdaQueryWrapper<TagEntity> wrapper = new LambdaQueryWrapper<TagEntity>().eq(TagEntity::getSlug, slug);
        if (currentId != null) {
            wrapper.ne(TagEntity::getId, currentId);
        }
        Long count = tagMapper.selectCount(wrapper);
        if (count > 0) {
            throw BusinessException.conflict("标签标识已存在");
        }
    }

    private TagEntity requireTag(Long id) {
        TagEntity tag = tagMapper.selectById(id);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在");
        }
        return tag;
    }

    private long referenceCount(Long id) {
        Long count = jdbcTemplate.queryForObject("""
                        select
                            (select count(*) from article_tags where tag_id = ?) +
                            (select count(*) from project_tags where tag_id = ?) +
                            (select count(*) from inspiration_tags where tag_id = ?) +
                            (select count(*) from tag_aliases where tag_id = ?)
                        """,
                Long.class,
                id,
                id,
                id,
                id
        );
        return count == null ? 0 : count;
    }

    private String normalizeSlug(String slug) {
        String normalized = slug == null ? "" : slug.trim().toLowerCase();
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw BusinessException.badRequest("标签标识只能使用小写字母、数字和连字符");
        }
        return normalized;
    }

    private String normalizeColor(String color) {
        String cleaned = clean(color);
        if (cleaned == null) {
            return null;
        }
        if (!COLOR_PATTERN.matcher(cleaned).matches()) {
            throw BusinessException.badRequest("标签颜色必须是合法的十六进制色值");
        }
        return cleaned;
    }

    private int normalizeWeight(Integer weight) {
        return weight == null ? 0 : weight;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String cleanRequired(String value, String message) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw BusinessException.badRequest(message);
        }
        return cleaned;
    }

    private TagVO toVO(TagEntity entity) {
        return new TagVO(entity.getId(), entity.getName(), entity.getSlug(), entity.getColor(), entity.getWeight());
    }
}
