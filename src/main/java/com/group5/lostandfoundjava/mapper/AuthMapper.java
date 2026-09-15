package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Assembles the reply that {@code /register}, {@code /login} and {@code /refresh} all share.
 *
 * <p>It delegates the profile part to {@link UserMapper} rather than rebuilding it, so the user
 * object inside an auth response is always identical to the one {@code /api/users/me} returns.
 */
@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final UserMapper userMapper;

    public AuthResponse toResponse(
            User user, String accessToken, String refreshToken, long expiresInSeconds) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInSeconds(expiresInSeconds)
                .user(userMapper.toResponse(user))
                .build();
    }
}
