package com.creatorspace.module.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 120) String slug,
        @Size(max = 32) String color,
        Integer weight
) {
}
