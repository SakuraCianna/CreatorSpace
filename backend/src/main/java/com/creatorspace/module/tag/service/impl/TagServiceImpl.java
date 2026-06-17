package com.creatorspace.module.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.entity.TagEntity;
import com.creatorspace.module.tag.mapper.TagMapper;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 标签业务实现，负责标签创建、排序查询和按主键回填。
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    // 通过构造器注入标签数据访问对象。
    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    // 创建标签并校验标签标识唯一性。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO create(TagRequest request) {
        String slug = request.slug().trim().toLowerCase();
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<TagEntity>().eq(TagEntity::getSlug, slug));
        if (count > 0) {
            throw BusinessException.conflict("标签标识已存在");
        }

        TagEntity tag = new TagEntity();
        tag.setName(request.name().trim());
        tag.setSlug(slug);
        tag.setColor(request.color());
        tag.setWeight(request.weight() == null ? 0 : request.weight());
        tagMapper.insert(tag);
        return toVO(tag);
    }

    // 按权重和名称查询标签列表。
    @Override
    public List<TagVO> list() {
        return tagMapper.selectList(new LambdaQueryWrapper<TagEntity>()
                        .orderByDesc(TagEntity::getWeight)
                        .orderByAsc(TagEntity::getName))
                .stream()
                .map(this::toVO)
                .toList();
    }

    // 按主键集合查询标签列表，供内容视图回填标签使用。
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

    // 转换为前端可用的视图对象。
    private TagVO toVO(TagEntity entity) {
        return new TagVO(entity.getId(), entity.getName(), entity.getSlug(), entity.getColor(), entity.getWeight());
    }
}
