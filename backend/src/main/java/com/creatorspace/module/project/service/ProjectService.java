package com.creatorspace.module.project.service;

import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.project.dto.ProjectCreateRequest;
import com.creatorspace.module.project.vo.ProjectFilterRecommendationVO;
import com.creatorspace.module.project.vo.ProjectVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 作品业务编排接口。
 */
public interface ProjectService {

    // 创建作品并写入标签绑定。
    ProjectVO create(ProjectCreateRequest request, Long operatorId);

    // 当前用户创建自己的作品草稿。
    ProjectVO createMine(ProjectCreateRequest request, Long userId);

    // 查询当前用户自己的作品列表。
    PageResponse<ProjectVO> listMine(Long userId, String keyword, String status, long page, long pageSize);

    // 当前用户读取自己的作品。
    ProjectVO getMineById(Long id, Long userId);

    // 当前用户更新自己的作品。
    ProjectVO updateMine(Long id, ProjectCreateRequest request, Long userId);

    // 当前用户提交自己的作品进入审核。
    ProjectVO submitForReview(Long id, Long userId);

    // 当前用户删除自己的未公开作品。
    void deleteMine(Long id, Long userId);

    // 管理员更新作品。
    ProjectVO update(Long id, ProjectCreateRequest request, Long operatorId);

    // 管理员删除作品。
    void delete(Long id);

    // 管理员设置作品展示状态。
    ProjectVO setStatus(Long id, String status, Long operatorId);

    // 管理员审核通过作品。
    ProjectVO approve(Long id, Long operatorId);

    // 管理员驳回作品。
    ProjectVO reject(Long id, String reviewNote, Long operatorId);

    // 管理员切换推荐状态。
    ProjectVO setRecommend(Long id, boolean enabled, Long operatorId);

    // 管理员查询全部作品。
    PageResponse<ProjectVO> listAdmin(String keyword, String status, long page, long pageSize);

    // 管理员按主键读取作品。
    ProjectVO getAdminById(Long id);

    // 查询公开作品列表。
    PageResponse<ProjectVO> listPublic(String keyword, long page, long pageSize);

    // 查询作品展厅推荐筛选项。
    ProjectFilterRecommendationVO recommendFilters(Long userId, String ipAddress, int limit);

    // 按 URL 标识读取公开作品。
    ProjectVO getPublicBySlug(String slug, HttpServletRequest request);
}
