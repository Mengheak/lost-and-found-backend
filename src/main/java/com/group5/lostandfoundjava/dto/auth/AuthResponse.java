package com.group5.lostandfoundjava.dto.auth;

import com.group5.lostandfoundjava.dto.user.UserResponse;

/**
 * What register, login and refresh all return.
 *
 * <p>Two tokens are issued: a short-lived {@code accessToken} sent with every request, and a
 * long-lived {@code refreshToken} used only to obtain a new pair once the access token expires.
 *
 * @param expiresInSeconds lifetime of the access token, so the client can refresh before it expires
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {

    /** Builds a response with the usual {@code "Bearer"} token type. */
    public static AuthResponse of(
            String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
