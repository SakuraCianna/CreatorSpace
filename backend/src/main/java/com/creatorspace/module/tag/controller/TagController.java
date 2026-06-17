package com.creatorspace.module.tag.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签管理和公开标签查询接口。
 */
@RestController
public class TagController {

    private final TagService tagService;

    // 通过构造器注入标签服务，便于保持控制层轻量。
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    // 创建标签并返回前端视图。
    @PostMapping("/api/admin/tags")
    public ApiResponse<TagVO> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.ok(tagService.create(request));
    }

    // 查询全部标签列表。
    @GetMapping("/api/tags")
    public ApiResponse<List<TagVO>> list() {
        return ApiResponse.ok(tagService.list());
    }
}
