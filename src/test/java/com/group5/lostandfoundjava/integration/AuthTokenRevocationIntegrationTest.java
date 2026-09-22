package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// End-to-end cover for the token store: taking a token back before it expires
class AuthTokenRevocationIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("full flow: register, call a protected endpoint, refresh, then log out")
    void fullAuthLifecycle() {
        JsonNode registered = registerUser("lifecycle@example.com", "Lifecycle");
        String accessToken = registered.path("accessToken").asText();
        String refreshToken = registered.path("refreshToken").asText();

        assertFalse(accessToken.isBlank());
        assertFalse(refreshToken.isBlank());
        assertEquals("Bearer", registered.path("tokenType").asText());
        assertTrue(registered.path("expiresInSeconds").asLong() > 0);

        // The token issued at registration works straight away
        ResponseEntity<String> me = getJson("/api/users/me", accessToken);
        assertEquals(HttpStatus.OK, me.getStatusCode());
        assertEquals("lifecycle@example.com", json(me).path("data").path("email").asText());

        // Refreshing hands back a usable new pair
        ResponseEntity<String> refreshed =
                postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null);
        assertEquals(HttpStatus.OK, refreshed.getStatusCode());
        String newAccessToken = json(refreshed).path("data").path("accessToken").asText();
        assertEquals(HttpStatus.OK, getJson("/api/users/me", newAccessToken).getStatusCode());

        // Logging out ends the session for the token that was just used
        ResponseEntity<String> logout = postJson("/api/auth/logout", null, newAccessToken);
        assertEquals(HttpStatus.OK, logout.getStatusCode());
        assertTrue(json(logout).path("success").asBoolean());

        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", newAccessToken).getStatusCode());
    }

    @Test
    @DisplayName("an access token stops working the moment its owner logs out")
    void logoutRevokesTheAccessToken() {
        String accessToken = registerUser("logout@example.com").path("accessToken").asText();
        assertEquals(HttpStatus.OK, getJson("/api/users/me", accessToken).getStatusCode());

        assertEquals(HttpStatus.OK, postJson("/api/auth/logout", null, accessToken).getStatusCode());

        // Same token, same signature, still inside its lifetime, and now refused
        ResponseEntity<String> after = getJson("/api/users/me", accessToken);
        assertEquals(HttpStatus.UNAUTHORIZED, after.getStatusCode());
        assertFalse(json(after).path("success").asBoolean());
    }

    @Test
    @DisplayName("logging out also kills the refresh token, so no new pair can be minted")
    void logoutRevokesTheRefreshToken() {
        JsonNode registered = registerUser("logout-refresh@example.com");
        String accessToken = registered.path("accessToken").asText();
        String refreshToken = registered.path("refreshToken").asText();

        assertEquals(HttpStatus.OK, postJson("/api/auth/logout", null, accessToken).getStatusCode());

        ResponseEntity<String> refreshed =
                postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null);
        assertEquals(HttpStatus.UNAUTHORIZED, refreshed.getStatusCode());
    }

    @Test
    @DisplayName("logging out requires a token of its own")
    void logoutRequiresAuthentication() {
        assertEquals(HttpStatus.UNAUTHORIZED, postJson("/api/auth/logout", null, null).getStatusCode());
    }

    @Test
    @DisplayName("a refresh token is rotated, so replaying the old one is refused")
    void refreshTokenIsRotated() {
        String refreshToken = registerUser("rotation@example.com").path("refreshToken").asText();

        ResponseEntity<String> first = postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null);
        assertEquals(HttpStatus.OK, first.getStatusCode());
        String rotatedRefreshToken = json(first).path("data").path("refreshToken").asText();
        assertNotEquals(refreshToken, rotatedRefreshToken);

        // Replaying the token that was just exchanged gets nowhere
        ResponseEntity<String> replay = postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null);
        assertEquals(HttpStatus.UNAUTHORIZED, replay.getStatusCode());

        // The rotated one is the live token and still works
        assertEquals(
                HttpStatus.OK,
                postJson("/api/auth/refresh", Map.of("refreshToken", rotatedRefreshToken), null)
                        .getStatusCode());
    }

    @Test
    @DisplayName("two simultaneous refreshes can consume a token only once")
    void concurrentRefreshAllowsExactlyOneWinner() throws Exception {
        String refreshToken = registerUser("concurrent-rotation@example.com")
                .path("refreshToken").asText();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var request = (java.util.concurrent.Callable<ResponseEntity<String>>) () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start concurrent refresh");
                }
                return postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null);
            };
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            HttpStatus firstStatus = HttpStatus.valueOf(first.get(10, TimeUnit.SECONDS).getStatusCode().value());
            HttpStatus secondStatus = HttpStatus.valueOf(second.get(10, TimeUnit.SECONDS).getStatusCode().value());
            long successes = java.util.stream.Stream.of(firstStatus, secondStatus)
                    .filter(HttpStatus.OK::equals).count();
            long rejections = java.util.stream.Stream.of(firstStatus, secondStatus)
                    .filter(HttpStatus.UNAUTHORIZED::equals).count();

            assertEquals(1, successes);
            assertEquals(1, rejections);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("rotating a refresh token leaves the current access token alone")
    void refreshDoesNotLogTheCallerOut() {
        JsonNode registered = registerUser("rotation-keeps-session@example.com");
        String accessToken = registered.path("accessToken").asText();
        String refreshToken = registered.path("refreshToken").asText();

        assertEquals(
                HttpStatus.OK,
                postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken), null)
                        .getStatusCode());

        assertEquals(HttpStatus.OK, getJson("/api/users/me", accessToken).getStatusCode());
    }

    @Test
    @DisplayName("logging in again ends the session the account already had")
    void loginRevokesTheEarlierSession() {
        String firstAccessToken =
                registerUser("relogin@example.com").path("accessToken").asText();
        assertEquals(HttpStatus.OK, getJson("/api/users/me", firstAccessToken).getStatusCode());

        ResponseEntity<String> login = postJson("/api/auth/login", credentials("relogin@example.com"), null);
        assertEquals(HttpStatus.OK, login.getStatusCode());
        String secondAccessToken = json(login).path("data").path("accessToken").asText();

        // The older session is gone, the new one works
        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", firstAccessToken).getStatusCode());
        assertEquals(HttpStatus.OK, getJson("/api/users/me", secondAccessToken).getStatusCode());
    }

    @Test
    @DisplayName("a refresh token cannot be used as an access token")
    void refreshTokenIsNotAnAccessToken() {
        String refreshToken =
                registerUser("wrong-token-kind@example.com").path("refreshToken").asText();

        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", refreshToken).getStatusCode());
    }

    @Test
    @DisplayName("a forged or unknown token is refused rather than trusted")
    void unknownTokenIsRefused() {
        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", "not-a-jwt-at-all").getStatusCode());
    }

    @Test
    @DisplayName("one account logging out leaves other accounts signed in")
    void logoutIsPerAccount() {
        String victimToken = registerUser("logout-a@example.com").path("accessToken").asText();
        String bystanderToken =
                registerUser("logout-b@example.com").path("accessToken").asText();

        assertEquals(HttpStatus.OK, postJson("/api/auth/logout", null, victimToken).getStatusCode());

        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", victimToken).getStatusCode());
        assertEquals(HttpStatus.OK, getJson("/api/users/me", bystanderToken).getStatusCode());
    }

    private Map<String, Object> credentials(String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", "password123");
        return body;
    }
}
