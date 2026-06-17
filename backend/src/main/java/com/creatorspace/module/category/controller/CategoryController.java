package com.creatorspace.module.category.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.module.category.dto.CategoryRequest;
import com.creatorspace.module.category.service.CategoryService;
import com.creatorspace.module.category.vo.CategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类管理和公开分类查询接口。
 */
@Validated
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    // 通过构造器注入分类服务，保持请求处理逻辑集中在 Service。
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 管理员创建内容分类。
    @PostMapping("/api/admin/categories")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    // 查询指定模块下的启用分类。
    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryVO>> list(@RequestParam @NotBlank String module) {
        return ApiResponse.ok(categoryService.listEnabled(module));
    }
}
