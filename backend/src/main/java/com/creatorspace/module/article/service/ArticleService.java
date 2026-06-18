package com.creatorspace.module.article.service;

import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.article.dto.ArticleCreateRequest;
import com.creatorspace.module.article.vo.ArticleVO;

/**
 * 文章业务编排接口。
 */
public interface ArticleService {

    // 创建文章草稿。
    ArticleVO create(ArticleCreateRequest request, Long operatorId);

    // 发布文章并更新状态。
    ArticleVO publish(Long id, Long operatorId);

    // 将文章撤回为草稿。
    ArticleVO unpublish(Long id, Long operatorId);

    // 管理员更新文章内容。
    ArticleVO update(Long id, ArticleCreateRequest request, Long operatorId);

    // 管理员删除文章。
    void delete(Long id);

    // 管理员切换置顶状态。
    ArticleVO setTop(Long id, boolean enabled, Long operatorId);

    // 管理员切换推荐状态。
    ArticleVO setRecommend(Long id, boolean enabled, Long operatorId);

    // 管理员查询全部文章列表。
    PageResponse<ArticleVO> listAdmin(String keyword, String status, long page, long pageSize);

    // 管理员按主键读取文章。
    ArticleVO getAdminById(Long id);

    // 查询公开文章列表。
    PageResponse<ArticleVO> listPublic(String keyword, long page, long pageSize);

    // 按 URL 标识读取公开文章。
    ArticleVO getPublicBySlug(String slug);
}
