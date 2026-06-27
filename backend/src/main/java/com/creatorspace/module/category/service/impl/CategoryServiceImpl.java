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
import java.util.regex.Pattern;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO create(CategoryRequest request) {
        String module = normalizeModule(request.module());
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(null, module, slug);

        CategoryEntity category = new CategoryEntity();
        category.setModule(module);
        category.setName(cleanRequired(request.name(), "分类名称不能为空"));
        category.setSlug(slug);
        category.setDescription(clean(request.description()));
        category.setSortOrder(normalizeSortOrder(request.sortOrder()));
        category.setEnabled(request.enabled() == null ? true : request.enabled());
        categoryMapper.insert(category);
        return toVO(category);
    }

    @Override
    public List<CategoryVO> listAdmin(String module) {
        String normalizedModule = normalizeModule(module);
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryEntity>()
                        .eq(CategoryEntity::getModule, normalizedModule)
                        .orderByAsc(CategoryEntity::getSortOrder)
                        .orderByDesc(CategoryEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO update(Long id, CategoryRequest request) {
        CategoryEntity category = requireCategory(id);
        String module = normalizeModule(request.module());
        String slug = normalizeSlug(request.slug());
        ensureSlugAvailable(id, module, slug);

        category.setModule(module);
        category.setName(cleanRequired(request.name(), "分类名称不能为空"));
        category.setSlug(slug);
        category.setDescription(clean(request.description()));
        category.setSortOrder(normalizeSortOrder(request.sortOrder()));
        if (request.enabled() != null) {
            category.setEnabled(request.enabled());
        }
        categoryMapper.updateById(category);
        return toVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO setEnabled(Long id, boolean enabled) {
        CategoryEntity category = requireCategory(id);
        category.setEnabled(enabled);
        categoryMapper.updateById(category);
        return toVO(category);
    }

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

    private void ensureSlugAvailable(Long currentId, String module, String slug) {
        LambdaQueryWrapper<CategoryEntity> wrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getModule, module)
                .eq(CategoryEntity::getSlug, slug);
        if (currentId != null) {
            wrapper.ne(CategoryEntity::getId, currentId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw BusinessException.conflict("分类标识已存在");
        }
    }

    private CategoryEntity requireCategory(Long id) {
        CategoryEntity category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }
        return category;
    }

    private String normalizeModule(String module) {
        String normalized = module == null ? "" : module.trim().toUpperCase();
        if (!ContentConstants.CATEGORY_MODULES.contains(normalized)) {
            throw BusinessException.badRequest("分类模块不合法");
        }
        return normalized;
    }

    private String normalizeSlug(String slug) {
        String normalized = slug == null ? "" : slug.trim().toLowerCase();
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw BusinessException.badRequest("分类标识只能使用小写字母、数字和连字符");
        }
        return normalized;
    }

    private int normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
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

    private CategoryVO toVO(CategoryEntity entity) {
        return new CategoryVO(
                entity.getId(),
                entity.getModule(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getEnabled()
        );
    }
}
