package com.creatorspace.module.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 30) String module,
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 120) String slug,
        @Size(max = 500) String description,
        Integer sortOrder,
        Boolean enabled
) {
}
