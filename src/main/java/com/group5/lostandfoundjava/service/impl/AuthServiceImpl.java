package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.exception.ConflictException;
import com.group5.lostandfoundjava.exception.TooManyRequestsException;
import com.group5.lostandfoundjava.exception.UnauthorizedException;
import com.group5.lostandfoundjava.mapper.AuthMapper;
import com.group5.lostandfoundjava.mapper.UserMapper;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.security.JwtProvider;
import com.group5.lostandfoundjava.service.AuthService;
import com.group5.lostandfoundjava.service.LoginAttemptService;
import com.group5.lostandfoundjava.service.TokenService;
import io.jsonwebtoken.Claims;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Email is already registered");
        }

        return issueTokens(userRepository.save(user));
    }

    // An unknown email and a wrong password produce exactly the same error
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Long remainingSeconds = loginAttemptService.lockoutSecondsRemaining(email);
        if (remainingSeconds != null) {
            long remainingMinutes = (remainingSeconds + 59L) / 60L;
            throw new TooManyRequestsException(
                    "Too many failed attempts. Try again in " + remainingMinutes + " minute(s).");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(email);
            throw new UnauthorizedException("Invalid email or password");
        }

        // The credentials are good, so the account is certain to exist by this point.
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        loginAttemptService.recordSuccess(email);

        // Signing in afresh ends the account's other sessions
        tokenService.revokeAll(user.getId());

        return issueTokens(user);
    }

    // The role is re-read from the database here rather than copied out of the refresh token
    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        Claims claims = jwtProvider.parse(refreshToken);
        if (claims == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!jwtProvider.isRefreshToken(claims)) {
            throw new UnauthorizedException("Provided token is not a refresh token");
        }
        // Checking and consuming happen in one conditional UPDATE. If simultaneous
        // requests present this token, only one can rotate it.
        if (!tokenService.consume(refreshToken)) {
            throw new UnauthorizedException("Refresh token is no longer valid");
        }

        User user = userRepository
                .findById(jwtProvider.userIdFrom(claims))
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        tokenService.revokeAll(userId);
    }

    // Issues a fresh pair, records both so they can be revoked, and builds the response
    private AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        tokenService.savePair(user, accessToken, refreshToken);

        return authMapper.toResponse(user, accessToken, refreshToken, jwtProvider.getAccessTokenTtlSeconds());
    }
}
