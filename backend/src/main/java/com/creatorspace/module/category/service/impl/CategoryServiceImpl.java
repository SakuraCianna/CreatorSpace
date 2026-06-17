package com.creatorspace.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.creatorspace.common.constant.ContentConstants;
import com.creatorspace.common.exception.BusinessException;
import com.creatorspace.module.category.dto.CategoryRequest;
import com.creatorspace.module.category.entity.CategoryEntity;
import com.creatorspace.module.category.mapper.CategoryMapper;
import com.creatorspace.module.category.service.CategoryService;
import com.creatorspace.module.category.vo.CategoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类业务实现，负责模块校验和启用分类查询。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    // 通过构造器注入分类数据访问对象。
    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    // 创建分类并保证同一模块内标识唯一。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO create(CategoryRequest request) {
        String module = normalizeModule(request.module());
        String slug = normalizeSlug(request.slug());
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getModule, module)
                .eq(CategoryEntity::getSlug, slug));
        if (count > 0) {
            throw BusinessException.conflict("分类标识已存在");
        }

        CategoryEntity category = new CategoryEntity();
        category.setModule(module);
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setEnabled(true);
        categoryMapper.insert(category);
        return toVO(category);
    }

    // 查询启用状态的分类列表。
    @Override
    public List<CategoryVO> listEnabled(String module) {
        String normalizedModule = normalizeModule(module);
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryEntity>()
                        .eq(CategoryEntity::getModule, normalizedModule)
                        .eq(CategoryEntity::getEnabled, true)
                        .orderByAsc(CategoryEntity::getSortOrder)
                        .orderByDesc(CategoryEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    // 按主键查询启用分类视图对象。
    @Override
    public CategoryVO findById(Long id) {
        if (id == null) {
            return null;
        }
        CategoryEntity category = categoryMapper.selectById(id);
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            throw BusinessException.notFound("分类不存在");
        }
        return toVO(category);
    }

    // 规范化并校验分类模块。
    private String normalizeModule(String module) {
        String normalized = module == null ? "" : module.trim().toUpperCase();
        if (!ContentConstants.CATEGORY_MODULES.contains(normalized)) {
            throw BusinessException.badRequest("分类模块不合法");
        }
        return normalized;
    }

    // 规范化 URL 标识。
    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }

    // 转换为前端可用的视图对象。
    private CategoryVO toVO(CategoryEntity entity) {
        return new CategoryVO(
                entity.getId(),
                entity.getModule(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getSortOrder()
        );
    }
}
