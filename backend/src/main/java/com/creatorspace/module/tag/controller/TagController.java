package com.creatorspace.module.tag.controller;

import com.creatorspace.common.result.ApiResponse;
import com.creatorspace.module.tag.dto.TagRequest;
import com.creatorspace.module.tag.service.TagService;
import com.creatorspace.module.tag.vo.TagVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping("/api/admin/tags")
    public ApiResponse<TagVO> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.ok(tagService.create(request));
    }

    @PutMapping("/api/admin/tags/{id}")
    public ApiResponse<TagVO> update(
            @PathVariable Long id,
            @Valid @RequestBody TagRequest request
    ) {
        return ApiResponse.ok(tagService.update(id, request));
    }

    @DeleteMapping("/api/admin/tags/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/tags")
    public ApiResponse<List<TagVO>> list() {
        return ApiResponse.ok(tagService.list());
    }
}
