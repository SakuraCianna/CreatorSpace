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

    // 查询公开文章列表。
    PageResponse<ArticleVO> listPublic(String keyword, long page, long pageSize);

    // 按 URL 标识读取公开文章。
    ArticleVO getPublicBySlug(String slug);
}
