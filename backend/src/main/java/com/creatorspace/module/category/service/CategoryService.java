package com.creatorspace.module.category.service;

import com.creatorspace.module.category.dto.CategoryRequest;
import com.creatorspace.module.category.vo.CategoryVO;

import java.util.List;

/**
 * 分类业务接口。
 */
public interface CategoryService {

    // 创建分类并校验模块和标识。
    CategoryVO create(CategoryRequest request);

    // 查询启用状态的分类列表。
    List<CategoryVO> listEnabled(String module);

    // 按主键查询分类视图对象。
    CategoryVO findById(Long id);
}
