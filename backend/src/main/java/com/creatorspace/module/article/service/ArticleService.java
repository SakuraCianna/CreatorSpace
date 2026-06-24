package com.creatorspace.module.article.service;

import com.creatorspace.common.result.PageResponse;
import com.creatorspace.module.article.dto.ArticleCreateRequest;
import com.creatorspace.module.article.vo.ArticleVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 文章业务编排接口。
 */
public interface ArticleService {

    // 创建文章草稿。
    ArticleVO create(ArticleCreateRequest request, Long operatorId);

    // 查询当前用户自己的文章列表。
    PageResponse<ArticleVO> listMine(Long userId, String keyword, String status, long page, long pageSize);

    // 当前用户读取自己的文章。
    ArticleVO getMineById(Long id, Long userId);

    // 当前用户更新自己的文章。
    ArticleVO updateMine(Long id, ArticleCreateRequest request, Long userId);

    // 当前用户提交自己的文章进入审核。
    ArticleVO submitForReview(Long id, Long userId);

    // 当前用户删除自己的未公开文章。
    void deleteMine(Long id, Long userId);

    // 发布文章并更新状态。
    ArticleVO publish(Long id, Long operatorId);

    // 将文章撤回为草稿。
    ArticleVO unpublish(Long id, Long operatorId);

    // 管理员审核通过投稿。
    ArticleVO approve(Long id, Long operatorId);

    // 管理员驳回投稿。
    ArticleVO reject(Long id, String reviewNote, Long operatorId);

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
    PageResponse<ArticleVO> listPublic(String keyword, Long tagId, long page, long pageSize);

    // 按 URL 标识读取公开文章。
    ArticleVO getPublicBySlug(String slug, HttpServletRequest request);
}
