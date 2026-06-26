package com.creatorspace.module.article.vo;

/**
 * 公开文章详情页的相邻文章快捷导航。
 */
public record ArticleNeighborsVO(
        ArticleVO previousArticle,
        ArticleVO nextArticle
) {
}
