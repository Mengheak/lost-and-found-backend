package com.group5.lostandfoundjava.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/refresh}. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
