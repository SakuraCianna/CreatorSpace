package com.creatorspace.module.tag.service;

import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.vo.TagVO;

import java.util.List;

public interface TagService {

    TagVO create(TagRequest request);

    TagVO update(Long id, TagRequest request);

    void delete(Long id);

    List<TagVO> list();

    List<TagVO> recommend(Long userId, String ipAddress, int limit);

    List<TagVO> listByIds(List<Long> tagIds);
}
