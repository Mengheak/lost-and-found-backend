package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import java.util.UUID;

/**
 * Registration, login, token refresh and logout.
 *
 * <p>Each service is split into an interface and an {@code impl} class. Controllers depend on the
 * interface, so the implementation can be swapped or mocked in a test without touching them.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    /** Revokes every token the user holds, ending all of their sessions at once. */
    void logout(UUID userId);
}
