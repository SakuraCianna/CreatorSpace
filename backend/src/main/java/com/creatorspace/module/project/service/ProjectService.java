package com.creatorspace.module.project.service;

import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.project.dto.ProjectCreateRequest;
import com.creatorspace.module.project.vo.ProjectVO;

/**
 * 作品业务编排接口。
 */
public interface ProjectService {

    // 创建作品并写入标签绑定。
    ProjectVO create(ProjectCreateRequest request, Long operatorId);

    // 管理员更新作品。
    ProjectVO update(Long id, ProjectCreateRequest request, Long operatorId);

    // 管理员删除作品。
    void delete(Long id);

    // 管理员设置作品展示状态。
    ProjectVO setStatus(Long id, String status, Long operatorId);

    // 管理员切换推荐状态。
    ProjectVO setRecommend(Long id, boolean enabled, Long operatorId);

    // 管理员查询全部作品。
    PageResponse<ProjectVO> listAdmin(String keyword, String status, long page, long pageSize);

    // 管理员按主键读取作品。
    ProjectVO getAdminById(Long id);

    // 查询公开作品列表。
    PageResponse<ProjectVO> listPublic(String keyword, long page, long pageSize);

    // 按 URL 标识读取公开作品。
    ProjectVO getPublicBySlug(String slug);
}
