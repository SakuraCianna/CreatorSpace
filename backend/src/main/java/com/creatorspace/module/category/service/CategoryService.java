package com.creatorspace.module.category.service;

import com.creatorspace.module.category.dto.CategoryRequest;
import com.creatorspace.module.category.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    CategoryVO create(CategoryRequest request);

    List<CategoryVO> listAdmin(String module);

    CategoryVO update(Long id, CategoryRequest request);

    CategoryVO setEnabled(Long id, boolean enabled);

    List<CategoryVO> listEnabled(String module);

    CategoryVO findById(Long id);
}
