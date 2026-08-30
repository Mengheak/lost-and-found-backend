package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.group5.lostandfoundjava.config.AdminProperties;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Proves the startup runner really does leave a usable admin behind. */
class DefaultAdminIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminProperties adminProperties;

    @Test
    @DisplayName("the default admin account exists with the ADMIN role")
    void defaultAdminExists() {
        Optional<User> admin = userRepository.findByEmail(adminProperties.email().toLowerCase());

        assertTrue(admin.isPresent(), "default admin was not created on startup");
        assertEquals(Role.ADMIN, admin.get().getRole());
    }

    @Test
    @DisplayName("the default admin can log in and reach the admin area")
    void defaultAdminCanLogIn() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", adminProperties.email());
        body.put("password", adminProperties.password());

        ResponseEntity<String> login = postJson("/api/auth/login", body, null);
        assertEquals(HttpStatus.OK, login.getStatusCode(), "default admin login failed: " + login.getBody());

        JsonNode data = json(login).path("data");
        assertEquals("ADMIN", data.path("user").path("role").asText());

        ResponseEntity<String> users =
                getJson("/api/admin/users", data.path("accessToken").asText());
        assertEquals(HttpStatus.OK, users.getStatusCode());
    }
}
