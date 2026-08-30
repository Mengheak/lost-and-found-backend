package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;

/**
 * Registration, login and token refresh.
 *
 * <p>Each service is split into an interface and an {@code impl} class. Controllers depend on the
 * interface, so the implementation can be swapped or mocked in a test without touching them.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);
}
