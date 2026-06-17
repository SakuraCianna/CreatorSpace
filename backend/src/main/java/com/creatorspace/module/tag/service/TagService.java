package com.creatorspace.module.tag.service;

import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.vo.TagVO;

import java.util.List;

/**
 * 标签业务接口。
 */
public interface TagService {

    // 创建标签并校验标签标识唯一性。
    TagVO create(TagRequest request);

    // 查询标签列表。
    List<TagVO> list();

    // 按主键集合查询标签列表，供文章和作品回填标签使用。
    List<TagVO> listByIds(List<Long> tagIds);
}
