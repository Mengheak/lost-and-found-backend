package com.group5.lostandfoundjava.dto.auth;

import com.group5.lostandfoundjava.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// What /register, /login and /refresh all return
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    // The scheme the client must put in front of the access token
    @Builder.Default // without this the builder ignores the initialiser and leaves the field null
    private String tokenType = "Bearer";

    private long expiresInSeconds;

    private UserResponse user;
}
