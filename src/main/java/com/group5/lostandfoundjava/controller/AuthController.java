package com.group5.lostandfoundjava.controller;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.dto.auth.AuthResponse;
import com.group5.lostandfoundjava.dto.auth.LoginRequest;
import com.group5.lostandfoundjava.dto.auth.RefreshTokenRequest;
import com.group5.lostandfoundjava.dto.auth.RegisterRequest;
import com.group5.lostandfoundjava.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three public endpoints under {@code /api/auth}.
 *
 * <p>Controllers stay thin on purpose: they receive the request, hand it to a service, and wrap the
 * result in the {@link ApiResponse} envelope. All the rules live in the service layer, where they
 * can be unit-tested without starting a web server.
 *
 * <p>Swagger has an annotation called {@code ApiResponse} too. Java cannot rename an import, so the
 * documentation annotations below are written out in full to keep them apart from our own
 * {@link ApiResponse} record. They describe the API in Swagger UI and change no behaviour.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new account",
            description = "Creates a user and immediately returns a token pair, so the client does not "
                    + "have to log in as a second step. The email must not already be registered.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Account created; tokens returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed (weak password, malformed email, ...)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Email is already registered")
    })
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Registered successfully");
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in with email and password",
            description = "Returns an access token and a refresh token. Repeated failures on the same "
                    + "email trigger a temporary lockout, after which even the correct password is refused "
                    + "until the window expires.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Logged in; tokens returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Invalid credentials. The same message is used for an unknown email "
                        + "and a wrong password, so the response cannot be used to probe for registered accounts."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "429",
                description = "Too many failed attempts; the account is temporarily locked")
    })
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Logged in successfully");
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Exchange a refresh token for a new token pair",
            description = "The role is re-read from the database on every refresh, so a promotion or "
                    + "demotion takes effect on the next refresh rather than only at the next login.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "New token pair issued"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Refresh token is missing, invalid or expired")
    })
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request), "Token refreshed");
    }
}
