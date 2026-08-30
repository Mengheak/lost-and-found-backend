package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.exception.ConflictException;
import com.group5.lostandfoundjava.exception.UnauthorizedException;
import com.group5.lostandfoundjava.config.JwtProperties;
import com.group5.lostandfoundjava.config.LoginThrottleProperties;
import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.security.JwtProvider;
import com.group5.lostandfoundjava.security.LoginAttemptService;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Unit tests for the sign-in rules.
 *
 * <p>The repository is a Mockito mock, so no database is involved and each test runs in
 * milliseconds. The password encoder and the JWT provider are the real thing, because the point of
 * several of these tests is that the hashing and the token contents really are correct.
 */
class AuthServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(
            "unit-test-secret-0123456789abcdef0123456789", Duration.ofMinutes(15), Duration.ofDays(7)));
    private final LoginAttemptService loginAttemptService =
            new LoginAttemptService(new LoginThrottleProperties(5, Duration.ofMinutes(15), Duration.ofMinutes(15)));

    private final AuthServiceImpl service =
            new AuthServiceImpl(userRepository, passwordEncoder, jwtProvider, loginAttemptService);

    @Test
    @DisplayName("register hashes the password, lowercases the email and returns both tokens")
    void registerHashesPasswordAndLowercasesEmail() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response =
                service.register(new RegisterRequest("Jane", "Jane@Example.com", null, "secret123"));

        assertEquals("jane@example.com", saved.getValue().getEmail());
        assertNotEquals("secret123", saved.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches("secret123", saved.getValue().getPasswordHash()));
        assertFalse(response.accessToken().isBlank());
        assertFalse(response.refreshToken().isBlank());
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    @DisplayName("register always creates a regular USER, never an admin")
    void registerAlwaysCreatesRegularUser() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(saved.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response =
                service.register(new RegisterRequest("Jane", "jane@example.com", null, "secret123"));

        assertEquals(Role.USER, saved.getValue().getRole());
        assertEquals(Role.USER, response.user().role());
    }

    @Test
    @DisplayName("register with an existing email throws ConflictException")
    void registerWithExistingEmailThrows() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.register(new RegisterRequest("Jane", "taken@example.com", null, "secret123")));
    }

    @Test
    @DisplayName("login with unknown email throws UnauthorizedException")
    void loginWithUnknownEmailThrows() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedException.class,
                () -> service.login(new LoginRequest("nobody@example.com", "whatever1")));
    }

    @Test
    @DisplayName("login with wrong password throws UnauthorizedException")
    void loginWithWrongPasswordThrows() {
        User user = user("correct-password", Role.USER);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(
                UnauthorizedException.class,
                () -> service.login(new LoginRequest(user.getEmail(), "wrong-password")));
    }

    @Test
    @DisplayName("login with correct credentials returns tokens")
    void loginWithCorrectCredentialsReturnsTokens() {
        User user = user("correct-password", Role.USER);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = service.login(new LoginRequest(user.getEmail(), "correct-password"));

        assertFalse(response.accessToken().isBlank());
        assertEquals(user.getId(), response.user().id());
    }

    @Test
    @DisplayName("refresh rejects an access token")
    void refreshRejectsAccessToken() {
        User user = user("irrelevant", Role.USER);
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());

        assertThrows(UnauthorizedException.class, () -> service.refresh(new RefreshTokenRequest(accessToken)));
    }

    @Test
    @DisplayName("login puts the user's role in the access token and the response")
    void loginPutsRoleInToken() {
        User user = user("correct-password", Role.ADMIN);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = service.login(new LoginRequest(user.getEmail(), "correct-password"));

        assertEquals(Role.ADMIN, response.user().role());
        Claims claims = jwtProvider.parse(response.accessToken());
        assertNotNull(claims);
        assertEquals(Role.ADMIN, jwtProvider.roleFrom(claims));
    }

    @Test
    @DisplayName("refresh re-reads the role from the database rather than the token")
    void refreshRereadsRoleFromDatabase() {
        User user = user("irrelevant", Role.ADMIN);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        // A refresh token deliberately carries no role, so it has to come from the database.
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        AuthResponse response = service.refresh(new RefreshTokenRequest(refreshToken));

        assertEquals(Role.ADMIN, response.user().role());
        assertEquals(Role.ADMIN, jwtProvider.roleFrom(jwtProvider.parse(response.accessToken())));
    }

    @Test
    @DisplayName("refresh with a valid refresh token returns a new token pair")
    void refreshWithValidTokenReturnsNewPair() {
        User user = user("irrelevant", Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        AuthResponse response = service.refresh(new RefreshTokenRequest(refreshToken));

        assertFalse(response.accessToken().isBlank());
        assertEquals(user.getId(), response.user().id());
    }

    private User user(String password, Role role) {
        return new User("Jane", "jane@example.com", null, passwordEncoder.encode(password), role);
    }
}
