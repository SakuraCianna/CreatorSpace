package com.creatorspace.module.article.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.article.dto.ArticleCreateRequest;
import com.creatorspace.module.article.service.ArticleService;
import com.creatorspace.module.article.vo.ArticleVO;
import com.creatorspace.security.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章管理和公开文章读取接口。
 */
@Validated
@RestController
public class ArticleController {

    private final ArticleService articleService;

    // 通过构造器注入文章服务，避免 Controller 承担业务编排。
    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // 管理员创建文章草稿。
    @PostMapping("/api/admin/articles")
    public ApiResponse<ArticleVO> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody ArticleCreateRequest request
    ) {
        return ApiResponse.ok(articleService.create(request, loginUser.userId()));
    }

    // 管理员发布指定文章。
    @PutMapping("/api/admin/articles/{id}/publish")
    public ApiResponse<ArticleVO> publish(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(articleService.publish(id, loginUser.userId()));
    }

    // 查询公开文章分页列表。
    @GetMapping("/api/articles")
    public ApiResponse<PageResponse<ArticleVO>> listPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(articleService.listPublic(keyword, page, pageSize));
    }

    // 按 URL 标识读取公开文章详情。
    @GetMapping("/api/articles/slug/{slug}")
    public ApiResponse<ArticleVO> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(articleService.getPublicBySlug(slug));
    }
}
