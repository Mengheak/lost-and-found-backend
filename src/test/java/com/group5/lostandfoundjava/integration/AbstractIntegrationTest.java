package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for the end-to-end tests: a real application on a random port, talking to a real
 * PostgreSQL started in Docker by Testcontainers.
 *
 * <p>{@code @ServiceConnection} points {@code spring.datasource.*} at that container automatically,
 * so Flyway runs the real migrations against the real database engine — which is the whole point.
 * An in-memory database would not prove the PostgreSQL-specific SQL works.
 *
 * <p>{@code disabledWithoutDocker} skips these tests instead of failing them on a machine with no
 * Docker daemon, so {@code ./mvnw test} still passes locally.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    protected ResponseEntity<String> postJson(String path, Object body, String token) {
        return exchange(path, HttpMethod.POST, body, token);
    }

    protected ResponseEntity<String> getJson(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    protected ResponseEntity<String> patchJson(String path, Object body, String token) {
        return exchange(path, HttpMethod.PATCH, body, token);
    }

    protected ResponseEntity<String> deleteJson(String path, String token) {
        return exchange(path, HttpMethod.DELETE, null, token);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, Object body, String token) {
        try {
            String json = body == null ? null : objectMapper.writeValueAsString(body);
            return restTemplate.exchange(
                    path, method, new HttpEntity<>(json, jsonHeaders(token)), String.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to call " + method + " " + path, ex);
        }
    }

    /** Parses the response body so tests can assert on individual JSON fields. */
    protected JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new IllegalStateException("Response was not valid JSON: " + response.getBody(), ex);
        }
    }

    /** Registers a user and returns the {@code data} node, which holds the tokens and the profile. */
    protected JsonNode registerUser(String email, String name) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", "password123");

        ResponseEntity<String> response = postJson("/api/auth/register", body, null);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "registration failed: " + response.getBody());
        return json(response).path("data");
    }

    protected JsonNode registerUser(String email) {
        return registerUser(email, "Test User");
    }
}
