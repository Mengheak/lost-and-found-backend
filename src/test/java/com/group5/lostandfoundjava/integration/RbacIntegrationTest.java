package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Who can reach what: public browsing, signed-in actions and the admin area. */
class RbacIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("new registrations are regular users")
    void newRegistrationsAreRegularUsers() {
        JsonNode registered = registerUser("rbac-plain@example.com");
        assertEquals("USER", registered.path("user").path("role").asText());
    }

    @Test
    @DisplayName("a regular user cannot reach the admin area")
    void regularUserCannotReachAdminArea() {
        String token = registerUser("rbac-user@example.com").path("accessToken").asText();

        assertEquals(HttpStatus.FORBIDDEN, getJson("/api/admin/users", token).getStatusCode());
    }

    @Test
    @DisplayName("an anonymous caller gets 401 rather than 403 from the admin area")
    void anonymousCallerGetsUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/admin/users", null).getStatusCode());
    }

    @Test
    @DisplayName("browsing reports and categories needs no account")
    void browsingIsPublic() {
        assertEquals(HttpStatus.OK, getJson("/api/items", null).getStatusCode());
        assertEquals(HttpStatus.OK, getJson("/api/categories", null).getStatusCode());
    }

    @Test
    @DisplayName("the personal endpoints stay private even though their prefixes are public")
    void personalEndpointsStayPrivate() {
        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/users/me", null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, getJson("/api/items/my", null).getStatusCode());
    }

    @Test
    @DisplayName("writes still require a token even where reads are public")
    void writesRequireToken() {
        assertEquals(
                HttpStatus.UNAUTHORIZED,
                postJson("/api/categories", Map.of("name", "Anonymous category attempt"), null)
                        .getStatusCode());
    }

    @Test
    @DisplayName("an admin can list users and sees the role field")
    void adminCanListUsers() {
        Admin admin = promoteToAdmin("rbac-admin-list@example.com");

        ResponseEntity<String> response = getJson("/api/admin/users", admin.token());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode content = json(response).path("data").path("content");
        assertTrue(content.isArray() && !content.isEmpty());
        content.forEach(node -> {
            String role = node.path("role").asText();
            assertTrue("USER".equals(role) || "ADMIN".equals(role), "unexpected role " + role);
        });
    }

    @Test
    @DisplayName("an admin can promote a regular user, who then reaches the admin area")
    void adminCanPromoteUser() {
        Admin admin = promoteToAdmin("rbac-promoter@example.com");
        JsonNode target = registerUser("rbac-promotable@example.com");
        String targetId = target.path("user").path("id").asText();

        assertEquals(
                HttpStatus.FORBIDDEN,
                getJson("/api/admin/users", target.path("accessToken").asText())
                        .getStatusCode());

        ResponseEntity<String> promotion =
                patchJson("/api/admin/users/" + targetId + "/role", Map.of("role", "ADMIN"), admin.token());
        assertEquals(HttpStatus.OK, promotion.getStatusCode());
        assertEquals("ADMIN", json(promotion).path("data").path("role").asText());

        // The new role only takes effect on the next token, which is what logging in again produces.
        ResponseEntity<String> login =
                postJson("/api/auth/login", credentials("rbac-promotable@example.com"), null);
        String newToken = json(login).path("data").path("accessToken").asText();
        assertEquals(HttpStatus.OK, getJson("/api/admin/users", newToken).getStatusCode());
    }

    @Test
    @DisplayName("an admin cannot change their own role")
    void adminCannotChangeOwnRole() {
        Admin admin = promoteToAdmin("rbac-self-demote@example.com");

        ResponseEntity<String> response =
                patchJson("/api/admin/users/" + admin.id() + "/role", Map.of("role", "USER"), admin.token());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("category writes are admin-only while reads stay open to any signed-in user")
    void categoryWritesAreAdminOnly() {
        String userToken =
                registerUser("rbac-category-user@example.com").path("accessToken").asText();
        Admin admin = promoteToAdmin("rbac-category-admin@example.com");

        assertEquals(HttpStatus.OK, getJson("/api/categories", userToken).getStatusCode());

        String name = "RBAC Test Category " + UUID.randomUUID();
        assertEquals(
                HttpStatus.FORBIDDEN,
                postJson("/api/categories", Map.of("name", name), userToken).getStatusCode());
        assertEquals(
                HttpStatus.CREATED,
                postJson("/api/categories", Map.of("name", name), admin.token()).getStatusCode());
    }

    private record Admin(String id, String token) {}

    /** Registers a user, promotes them straight in the database, then logs in for a fresh token. */
    private Admin promoteToAdmin(String email) {
        JsonNode registered = registerUser(email);
        String id = registered.path("user").path("id").asText();

        User user = userRepository.findById(UUID.fromString(id)).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        ResponseEntity<String> login = postJson("/api/auth/login", credentials(email), null);
        assertEquals(HttpStatus.OK, login.getStatusCode(), "admin login failed: " + login.getBody());
        return new Admin(id, json(login).path("data").path("accessToken").asText());
    }

    private Map<String, Object> credentials(String email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", "password123");
        return body;
    }
}
