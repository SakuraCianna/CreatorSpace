package com.creatorspace.module.category.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.module.category.dto.CategoryRequest;
import com.creatorspace.module.category.service.CategoryService;
import com.creatorspace.module.category.vo.CategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/api/admin/categories")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @GetMapping("/api/admin/categories")
    public ApiResponse<List<CategoryVO>> listAdmin(@RequestParam @NotBlank String module) {
        return ApiResponse.ok(categoryService.listAdmin(module));
    }

    @PutMapping("/api/admin/categories/{id}")
    public ApiResponse<CategoryVO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ApiResponse.ok(categoryService.update(id, request));
    }

    @PutMapping("/api/admin/categories/{id}/enabled")
    public ApiResponse<CategoryVO> setEnabled(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean enabled
    ) {
        return ApiResponse.ok(categoryService.setEnabled(id, enabled));
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryVO>> list(@RequestParam @NotBlank String module) {
        return ApiResponse.ok(categoryService.listEnabled(module));
    }
}
