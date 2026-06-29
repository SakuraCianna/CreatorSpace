package com.creatorspace.module.project.vo;

import java.util.List;

/**
 * 作品展厅筛选项推荐结果。
 */
public record ProjectFilterRecommendationVO(
        List<String> projectTypes,
        List<String> techStacks
) {
}
