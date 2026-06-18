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

    // 查询公开作品列表。
    PageResponse<ProjectVO> listPublic(String keyword, long page, long pageSize);

    // 按 URL 标识读取公开作品。
    ProjectVO getPublicBySlug(String slug);
}
