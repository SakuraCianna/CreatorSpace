package com.creatorspace.module.project.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.project.dto.ProjectCreateRequest;
import com.creatorspace.module.project.vo.ProjectFilterRecommendationVO;
import com.creatorspace.module.project.service.ProjectService;
import com.creatorspace.module.project.vo.ProjectVO;
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
 * 作品管理和公开作品读取接口。
 */
@Validated
@RestController
public class ProjectController {

    private final ProjectService projectService;

    // 通过构造器注入作品服务，Controller 不直接访问数据库。
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // 管理员创建作品草稿。
    @PostMapping("/api/admin/projects")
    public ApiResponse<ProjectVO> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        return ApiResponse.ok(projectService.create(request, loginUser.userId()));
    }

    // 登录创作者创建自己的作品草稿。
    @PostMapping("/api/creator/projects")
    public ApiResponse<ProjectVO> createMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        return ApiResponse.ok(projectService.createMine(request, loginUser.userId()));
    }

    // 登录创作者查询自己的作品队列。
    @GetMapping("/api/creator/projects")
    public ApiResponse<PageResponse<ProjectVO>> listMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(projectService.listMine(loginUser.userId(), keyword, status, page, pageSize));
    }

    // 登录创作者读取自己的作品详情。
    @GetMapping("/api/creator/projects/{id}")
    public ApiResponse<ProjectVO> getMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(projectService.getMineById(id, loginUser.userId()));
    }

    // 登录创作者更新自己的草稿或驳回作品。
    @PutMapping("/api/creator/projects/{id}")
    public ApiResponse<ProjectVO> updateMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        return ApiResponse.ok(projectService.updateMine(id, request, loginUser.userId()));
    }

    // 登录创作者提交作品进入审核。
    @PutMapping("/api/creator/projects/{id}/submit")
    public ApiResponse<ProjectVO> submitMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(projectService.submitForReview(id, loginUser.userId()));
    }

    // 登录创作者删除自己的未公开作品。
    @DeleteMapping("/api/creator/projects/{id}")
    public ApiResponse<Void> deleteMine(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        projectService.deleteMine(id, loginUser.userId());
        return ApiResponse.ok(null);
    }

    // 管理员查询全部作品。
    @GetMapping("/api/admin/projects")
    public ApiResponse<PageResponse<ProjectVO>> listAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(projectService.listAdmin(keyword, status, page, pageSize));
    }

    // 管理员读取单个作品，包含 Markdown 正文。
    @GetMapping("/api/admin/projects/{id}")
    public ApiResponse<ProjectVO> getAdmin(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getAdminById(id));
    }

    // 管理员更新作品。
    @PutMapping("/api/admin/projects/{id}")
    public ApiResponse<ProjectVO> update(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        return ApiResponse.ok(projectService.update(id, request, loginUser.userId()));
    }

    // 管理员切换作品展示状态。
    @PutMapping("/api/admin/projects/{id}/status")
    public ApiResponse<ProjectVO> setStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ApiResponse.ok(projectService.setStatus(id, status, loginUser.userId()));
    }

    // 管理员审核通过作品。
    @PutMapping("/api/admin/projects/{id}/approve")
    public ApiResponse<ProjectVO> approve(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(projectService.approve(id, loginUser.userId()));
    }

    // 管理员驳回作品。
    @PutMapping("/api/admin/projects/{id}/reject")
    public ApiResponse<ProjectVO> reject(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewRequest request
    ) {
        return ApiResponse.ok(projectService.reject(id, request == null ? null : request.reviewNote(), loginUser.userId()));
    }

    // 管理员切换推荐状态。
    @PutMapping("/api/admin/projects/{id}/recommend")
    public ApiResponse<ProjectVO> setRecommend(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean enabled
    ) {
        return ApiResponse.ok(projectService.setRecommend(id, enabled, loginUser.userId()));
    }

    // 管理员删除作品。
    @DeleteMapping("/api/admin/projects/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.ok(null);
    }

    // 查询公开作品分页列表。
    @GetMapping("/api/projects")
    public ApiResponse<PageResponse<ProjectVO>> listPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize
    ) {
        return ApiResponse.ok(projectService.listPublic(keyword, page, pageSize));
    }

    // 查询作品展厅推荐筛选项。
    @GetMapping("/api/projects/recommended-filters")
    public ApiResponse<ProjectFilterRecommendationVO> recommendFilters(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "12") @Min(1) @Max(60) int limit,
            HttpServletRequest request
    ) {
        Long userId = loginUser == null ? null : loginUser.userId();
        return ApiResponse.ok(projectService.recommendFilters(userId, request.getRemoteAddr(), limit));
    }

    // 按 URL 标识读取公开作品详情。
    @GetMapping("/api/projects/slug/{slug}")
    public ApiResponse<ProjectVO> getBySlug(@PathVariable String slug, HttpServletRequest request) {
        return ApiResponse.ok(projectService.getPublicBySlug(slug, request));
    }

    public record ReviewRequest(String reviewNote) {
    }
}
