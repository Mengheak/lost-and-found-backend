package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.exception.ConflictException;
import com.group5.lostandfoundjava.common.exception.TooManyRequestsException;
import com.group5.lostandfoundjava.common.exception.UnauthorizedException;
import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.security.JwtProvider;
import com.group5.lostandfoundjava.security.LoginAttemptService;
import com.group5.lostandfoundjava.service.AuthService;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final LoginAttemptService loginAttemptService;

    /**
     * Spring passes these in automatically ("constructor injection"). Making the fields {@code final}
     * guarantees they are set exactly once and never change.
     */
    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Creates the account and returns tokens straight away, so the client does not have to log in as
     * a second step. New accounts are always plain {@code USER}s — there is no way to register as an
     * admin.
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = userRepository.save(new User(
                request.name().trim(),
                email,
                request.phone() == null ? null : request.phone().trim(),
                passwordEncoder.encode(request.password()),
                Role.USER));

        return toAuthResponse(user);
    }

    /**
     * An unknown email and a wrong password produce exactly the same error. Saying "no such user"
     * would let anyone check which email addresses are registered here.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Long lockoutSeconds = loginAttemptService.lockoutSecondsRemaining(email);
        if (lockoutSeconds != null) {
            long minutes = (lockoutSeconds + 59) / 60;
            throw new TooManyRequestsException("Too many failed attempts. Try again in " + minutes + " minute(s).");
        }

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
            loginAttemptService.recordFailure(email);
            throw new UnauthorizedException("Invalid email or password");
        }

        loginAttemptService.recordSuccess(email);
        return toAuthResponse(found.get());
    }

    /**
     * The role is re-read from the database here rather than copied out of the refresh token, so a
     * promotion or demotion takes effect at the next refresh instead of only at the next login.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtProvider.parse(request.refreshToken());
        if (claims == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!jwtProvider.isRefreshToken(claims)) {
            throw new UnauthorizedException("Provided token is not a refresh token");
        }

        User user = userRepository
                .findById(jwtProvider.userIdFrom(claims))
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.of(
                jwtProvider.generateAccessToken(user.getId(), user.getRole()),
                jwtProvider.generateRefreshToken(user.getId()),
                jwtProvider.getAccessTokenTtlSeconds(),
                UserResponse.from(user));
    }
}
