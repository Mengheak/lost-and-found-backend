package com.group5.lostandfoundjava.dto.auth;

import com.group5.lostandfoundjava.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What {@code /register}, {@code /login} and {@code /refresh} all return. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    /**
     * The scheme the client must put in front of the access token. Always {@code Bearer}; it is sent
     * anyway so a client can build the {@code Authorization} header without hard-coding it.
     */
    @Builder.Default // without this the builder ignores the initialiser and leaves the field null
    private String tokenType = "Bearer";

    private long expiresInSeconds;

    private UserResponse user;
}
