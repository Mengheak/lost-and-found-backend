package com.group5.lostandfoundjava.dto.user;

import jakarta.validation.constraints.Size;


public record UpdateProfileRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 50) String phone,
        @Size(max = 1024) String profilePhotoUrl) {}
