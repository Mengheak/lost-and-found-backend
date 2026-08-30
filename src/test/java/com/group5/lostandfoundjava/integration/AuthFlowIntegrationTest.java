package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Register, log in, refresh, and the brute-force lockout — over real HTTP. */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("register, login and fetch own profile")
    void registerLoginAndFetchProfile() {
        var registered = registerUser("auth-flow@example.com", "Auth Flow");
        assertFalse(registered.path("accessToken").asText().isBlank());
        assertFalse(registered.path("refreshToken").asText().isBlank());

        ResponseEntity<String> login = postJson("/api/auth/login", credentials("auth-flow@example.com"), null);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        String token = json(login).path("data").path("accessToken").asText();

        ResponseEntity<String> me = getJson("/api/users/me", token);
        assertEquals(HttpStatus.OK, me.getStatusCode());
        assertEquals("auth-flow@example.com", json(me).path("data").path("email").asText());
    }

    @Test
    @DisplayName("registering the same email twice returns 409")
    void duplicateRegistrationReturnsConflict() {
        registerUser("duplicate@example.com");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Dup");
        body.put("email", "duplicate@example.com");
        body.put("password", "password123");

        ResponseEntity<String> second = postJson("/api/auth/register", body, null);
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
        assertFalse(json(second).path("success").asBoolean());
    }

    @Test
    @DisplayName("login with a wrong password returns 401")
    void wrongPasswordReturnsUnauthorized() {
        registerUser("wrong-pass@example.com");

        ResponseEntity<String> login =
                postJson("/api/auth/login", credentials("wrong-pass@example.com", "not-the-password"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, login.getStatusCode());
    }

    @Test
    @DisplayName("protected endpoints require authentication")
    void protectedEndpointsRequireAuthentication() {
        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", null).getStatusCode());
    }

    @Test
    @DisplayName("registration with an invalid body returns 400 with field errors")
    void invalidRegistrationReturnsBadRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "");
        body.put("email", "not-an-email");
        body.put("password", "short");

        ResponseEntity<String> response = postJson("/api/auth/register", body, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(json(response).path("success").asBoolean());
    }

    @Test
    @DisplayName("repeated wrong passwords lock the account with 429")
    void repeatedFailuresLockTheAccount() {
        registerUser("throttled@example.com");

        // The default limit is five failures, so the fifth one still answers 401 and locks.
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response =
                    postJson("/api/auth/login", credentials("throttled@example.com", "wrong-password"), null);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        // Even the correct password is now refused until the lockout expires.
        ResponseEntity<String> afterLockout =
                postJson("/api/auth/login", credentials("throttled@example.com"), null);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, afterLockout.getStatusCode());
        assertFalse(json(afterLockout).path("success").asBoolean());
    }

    @Test
    @DisplayName("throttling one account does not affect another")
    void throttlingIsPerAccount() {
        registerUser("victim@example.com");
        registerUser("bystander@example.com");

        for (int i = 0; i < 6; i++) {
            postJson("/api/auth/login", credentials("victim@example.com", "wrong-password"), null);
        }

        ResponseEntity<String> bystander =
                postJson("/api/auth/login", credentials("bystander@example.com"), null);
        assertEquals(HttpStatus.OK, bystander.getStatusCode());
    }

    @Test
    @DisplayName("refresh token can be exchanged for a new token pair")
    void refreshTokenCanBeExchanged() {
        var registered = registerUser("refresh@example.com");
        String refreshToken = registered.path("refreshToken").asText();

        ResponseEntity<String> refreshed =
                postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null);
        assertEquals(HttpStatus.OK, refreshed.getStatusCode());
        assertTrue(json(refreshed)
                .path("data")
                .path("accessToken")
                .asText()
                .length()
                > 0);
    }

    private Map<String, Object> credentials(String email) {
        return credentials(email, "password123");
    }

    private Map<String, Object> credentials(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        return body;
    }
}
