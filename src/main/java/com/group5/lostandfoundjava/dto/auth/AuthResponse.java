package com.group5.lostandfoundjava.dto.auth;

import com.group5.lostandfoundjava.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {

    public static AuthResponse of(
            String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
