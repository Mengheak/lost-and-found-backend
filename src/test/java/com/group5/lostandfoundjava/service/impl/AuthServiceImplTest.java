package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.config.JwtProperties;
import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.exception.ConflictException;
import com.group5.lostandfoundjava.exception.UnauthorizedException;
import com.group5.lostandfoundjava.mapper.AuthMapper;
import com.group5.lostandfoundjava.mapper.UserMapper;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.security.JwtProvider;
import com.group5.lostandfoundjava.service.TokenService;
import com.group5.lostandfoundjava.service.LoginAttemptService;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Unit tests for the sign-in rules
class AuthServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProvider jwtProvider = new JwtProvider(new JwtProperties(
            "unit-test-secret-0123456789abcdef0123456789", Duration.ofMinutes(15), Duration.ofDays(7)));
    private final UserMapper userMapper = new UserMapper();
    private final AuthMapper authMapper = new AuthMapper(userMapper);

    // Same wiring as ApplicationConfig, built by hand over the mocked repository
    private final AuthenticationManager authenticationManager = buildAuthenticationManager();

    private final AuthServiceImpl service = new AuthServiceImpl(
            userRepository,
            passwordEncoder,
            jwtProvider,
            authenticationManager,
            tokenService,
            userMapper,
            authMapper,
            loginAttemptService);

    private AuthenticationManager buildAuthenticationManager() {
        UserDetailsService userDetailsService = email -> userRepository
                .findByEmail(email == null ? null : email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @BeforeEach
    void allowTokensByDefault() {
        // Unless a test says otherwise, this caller wins the atomic token consumption.
        when(tokenService.consume(anyString())).thenReturn(true);
        when(tokenService.isActive(anyString())).thenReturn(true);
        when(loginAttemptService.lockoutSecondsRemaining(anyString())).thenReturn(null);
    }

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
        assertFalse(response.getAccessToken().isBlank());
        assertFalse(response.getRefreshToken().isBlank());
        assertEquals("jane@example.com", response.getUser().getEmail());
        assertEquals("Bearer", response.getTokenType());
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
        assertEquals(Role.USER, response.getUser().getRole());
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
    @DisplayName("register records both issued tokens so they can be revoked later")
    void registerRecordsIssuedTokens() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response =
                service.register(new RegisterRequest("Jane", "jane@example.com", null, "secret123"));

        verify(tokenService)
                .savePair(any(User.class), eq(response.getAccessToken()), eq(response.getRefreshToken()));
    }

    @Test
    @DisplayName("login with unknown email throws UnauthorizedException")
    void loginWithUnknownEmailThrows() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedException.class,
                () -> service.login(new LoginRequest("nobody@example.com", "whatever1")));
        verify(loginAttemptService).recordFailure("nobody@example.com");
    }

    @Test
    @DisplayName("login with wrong password throws UnauthorizedException")
    void loginWithWrongPasswordThrows() {
        User user = user("correct-password", Role.USER);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(
                UnauthorizedException.class,
                () -> service.login(new LoginRequest(user.getEmail(), "wrong-password")));
        verify(loginAttemptService).recordFailure(user.getEmail());
    }

    @Test
    @DisplayName("login with correct credentials returns tokens")
    void loginWithCorrectCredentialsReturnsTokens() {
        User user = user("correct-password", Role.USER);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = service.login(new LoginRequest(user.getEmail(), "correct-password"));

        assertFalse(response.getAccessToken().isBlank());
        assertEquals(user.getId(), response.getUser().getId());
        verify(loginAttemptService).recordSuccess(user.getEmail());
    }

    @Test
    @DisplayName("a locked email is rejected before credentials are checked")
    void lockedEmailIsRejectedBeforeAuthentication() {
        when(loginAttemptService.lockoutSecondsRemaining("jane@example.com")).thenReturn(61L);

        var exception = assertThrows(
                com.group5.lostandfoundjava.exception.TooManyRequestsException.class,
                () -> service.login(new LoginRequest(" Jane@Example.COM ", "correct-password")));

        assertEquals("Too many failed attempts. Try again in 2 minute(s).", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("logging in revokes the sessions the account already had")
    void loginRevokesPreviousSessions() {
        User user = user("correct-password", Role.USER);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.login(new LoginRequest(user.getEmail(), "correct-password"));

        verify(tokenService).revokeAll(user.getId());
    }

    @Test
    @DisplayName("a failed login revokes nothing")
    void failedLoginRevokesNothing() {
        User user = user("correct-password", Role.USER);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(
                UnauthorizedException.class,
                () -> service.login(new LoginRequest(user.getEmail(), "wrong-password")));

        verify(tokenService, never()).revokeAll(any(UUID.class));
    }

    @Test
    @DisplayName("refresh rejects an access token")
    void refreshRejectsAccessToken() {
        User user = user("irrelevant", Role.USER);
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());

        assertThrows(UnauthorizedException.class, () -> service.refresh(new RefreshTokenRequest(accessToken)));
    }

    @Test
    @DisplayName("refresh rejects a refresh token that has been revoked")
    void refreshRejectsRevokedToken() {
        User user = user("irrelevant", Role.USER);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        when(tokenService.consume(refreshToken)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> service.refresh(new RefreshTokenRequest(refreshToken)));
    }

    @Test
    @DisplayName("refresh rotates the presented token rather than leaving it usable")
    void refreshRotatesThePresentedToken() {
        User user = user("irrelevant", Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        service.refresh(new RefreshTokenRequest(refreshToken));

        verify(tokenService).consume(refreshToken);
        // Rotation must not log the account out of its other devices
        verify(tokenService, never()).revokeAll(any(UUID.class));
    }

    @Test
    @DisplayName("login puts the user's role in the access token and the response")
    void loginPutsRoleInToken() {
        User user = user("correct-password", Role.ADMIN);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = service.login(new LoginRequest(user.getEmail(), "correct-password"));

        assertEquals(Role.ADMIN, response.getUser().getRole());
        Claims claims = jwtProvider.parse(response.getAccessToken());
        assertNotNull(claims);
        assertEquals(Role.ADMIN, jwtProvider.roleFrom(claims));
    }

    @Test
    @DisplayName("refresh re-reads the role from the database rather than the token")
    void refreshRereadsRoleFromDatabase() {
        User user = user("irrelevant", Role.ADMIN);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        // A refresh token carries no role, so it has to come from the database
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        AuthResponse response = service.refresh(new RefreshTokenRequest(refreshToken));

        assertEquals(Role.ADMIN, response.getUser().getRole());
        assertEquals(Role.ADMIN, jwtProvider.roleFrom(jwtProvider.parse(response.getAccessToken())));
    }

    @Test
    @DisplayName("refresh with a valid refresh token returns a new token pair")
    void refreshWithValidTokenReturnsNewPair() {
        User user = user("irrelevant", Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        AuthResponse response = service.refresh(new RefreshTokenRequest(refreshToken));

        assertFalse(response.getAccessToken().isBlank());
        assertEquals(user.getId(), response.getUser().getId());
    }

    @Test
    @DisplayName("logout revokes every token the account holds")
    void logoutRevokesEverything() {
        User user = user("irrelevant", Role.USER);

        service.logout(user.getId());

        verify(tokenService).revokeAll(user.getId());
    }

    private User user(String password, Role role) {
        return new User("Jane", "jane@example.com", null, passwordEncoder.encode(password), role);
    }
}
