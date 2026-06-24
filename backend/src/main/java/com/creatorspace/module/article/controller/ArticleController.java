package com.creatorspace.module.article.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.article.dto.ArticleCreateRequest;
import com.creatorspace.module.article.service.ArticleService;
import com.creatorspace.module.article.vo.ArticleVO;
import com.creatorspace.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    // 登录创作者创建自己的文章草稿。
    @PostMapping("/api/creator/articles")
    public ApiResponse<ArticleVO> createMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody ArticleCreateRequest request
    ) {
        return ApiResponse.ok(articleService.create(request, loginUser.userId()));
    }

    // 登录创作者查询自己的文章队列。
    @GetMapping("/api/creator/articles")
    public ApiResponse<PageResponse<ArticleVO>> listMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(articleService.listMine(loginUser.userId(), keyword, status, page, pageSize));
    }

    // 登录创作者读取自己的文章详情。
    @GetMapping("/api/creator/articles/{id}")
    public ApiResponse<ArticleVO> getMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(articleService.getMineById(id, loginUser.userId()));
    }

    // 登录创作者更新自己的草稿或驳回文章。
    @PutMapping("/api/creator/articles/{id}")
    public ApiResponse<ArticleVO> updateMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @Valid @RequestBody ArticleCreateRequest request
    ) {
        return ApiResponse.ok(articleService.updateMine(id, request, loginUser.userId()));
    }

    // 登录创作者提交文章进入审核。
    @PutMapping("/api/creator/articles/{id}/submit")
    public ApiResponse<ArticleVO> submitMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(articleService.submitForReview(id, loginUser.userId()));
    }

    // 登录创作者删除自己的未公开文章。
    @DeleteMapping("/api/creator/articles/{id}")
    public ApiResponse<Void> deleteMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        articleService.deleteMine(id, loginUser.userId());
        return ApiResponse.ok(null);
    }

    // 管理员查询全部文章。
    @GetMapping("/api/admin/articles")
    public ApiResponse<PageResponse<ArticleVO>> listAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(articleService.listAdmin(keyword, status, page, pageSize));
    }

    // 管理员读取单篇文章，包含 Markdown 正文。
    @GetMapping("/api/admin/articles/{id}")
    public ApiResponse<ArticleVO> getAdmin(@PathVariable Long id) {
        return ApiResponse.ok(articleService.getAdminById(id));
    }

    // 管理员更新文章内容。
    @PutMapping("/api/admin/articles/{id}")
    public ApiResponse<ArticleVO> update(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @Valid @RequestBody ArticleCreateRequest request
    ) {
        return ApiResponse.ok(articleService.update(id, request, loginUser.userId()));
    }

    // 管理员发布指定文章。
    @PutMapping("/api/admin/articles/{id}/publish")
    public ApiResponse<ArticleVO> publish(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(articleService.publish(id, loginUser.userId()));
    }

    // 管理员撤回文章到草稿。
    @PutMapping("/api/admin/articles/{id}/unpublish")
    public ApiResponse<ArticleVO> unpublish(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(articleService.unpublish(id, loginUser.userId()));
    }

    // 管理员审核通过投稿文章。
    @PutMapping("/api/admin/articles/{id}/approve")
    public ApiResponse<ArticleVO> approve(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(articleService.approve(id, loginUser.userId()));
    }

    // 管理员驳回投稿文章。
    @PutMapping("/api/admin/articles/{id}/reject")
    public ApiResponse<ArticleVO> reject(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequest request
    ) {
        return ApiResponse.ok(articleService.reject(id, request == null ? null : request.reviewNote(), loginUser.userId()));
    }

    // 管理员切换文章置顶状态。
    @PutMapping("/api/admin/articles/{id}/top")
    public ApiResponse<ArticleVO> setTop(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean enabled
    ) {
        return ApiResponse.ok(articleService.setTop(id, enabled, loginUser.userId()));
    }

    // 管理员切换文章推荐状态。
    @PutMapping("/api/admin/articles/{id}/recommend")
    public ApiResponse<ArticleVO> setRecommend(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean enabled
    ) {
        return ApiResponse.ok(articleService.setRecommend(id, enabled, loginUser.userId()));
    }

    // 管理员删除文章。
    @DeleteMapping("/api/admin/articles/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.ok(null);
    }

    // 查询公开文章分页列表。
    @GetMapping("/api/articles")
    public ApiResponse<PageResponse<ArticleVO>> listPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(articleService.listPublic(keyword, tagId, page, pageSize));
    }

    // 按 URL 标识读取公开文章详情。
    @GetMapping("/api/articles/slug/{slug}")
    public ApiResponse<ArticleVO> getBySlug(@PathVariable String slug, HttpServletRequest request) {
        return ApiResponse.ok(articleService.getPublicBySlug(slug, request));
    }

    public record ReviewRequest(String reviewNote) {
    }
}
