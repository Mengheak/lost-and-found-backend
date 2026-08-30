package com.group5.lostandfoundjava.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body used both to create and to rename a category. Admins only. */
public record CategoryRequest(
        @NotBlank @Size(max = 100) String name, @Size(max = 1024) String iconUrl) {}
