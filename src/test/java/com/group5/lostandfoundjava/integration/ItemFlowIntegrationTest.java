package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** The whole happy path: report an item, find it, save it, chat about it, return it, rate it. */
class ItemFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("categories are seeded by Flyway")
    void categoriesAreSeeded() {
        String token = registerUser("categories@example.com").path("accessToken").asText();

        ResponseEntity<String> response = getJson("/api/categories", token);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<String> names = new ArrayList<>();
        json(response).path("data").forEach(node -> names.add(node.path("name").asText()));

        assertEquals(13, names.size());
        assertTrue(names.contains("Wallet") && names.contains("Other"));
    }

    @Test
    @DisplayName("report, search, save, chat about and rate an item")
    void fullItemLifecycle() {
        JsonNode owner = registerUser("owner@example.com", "Olivia Owner");
        JsonNode finder = registerUser("finder@example.com", "Frank Finder");
        String ownerToken = owner.path("accessToken").asText();
        String finderToken = finder.path("accessToken").asText();
        String ownerId = owner.path("user").path("id").asText();

        String categoryId = categoryIdNamed(ownerToken, "Wallet");

        Map<String, Object> newItem = new LinkedHashMap<>();
        newItem.put("type", "LOST");
        newItem.put("name", "Black leather wallet");
        newItem.put("categoryId", categoryId);
        newItem.put("description", "Lost near the central station");
        newItem.put("brand", "Fossil");
        newItem.put("color", "black");
        newItem.put("rewardAmount", 25.50);
        newItem.put("photoUrls", List.of("https://example.com/wallet.jpg"));

        ResponseEntity<String> created = postJson("/api/items", newItem, ownerToken);
        assertEquals(HttpStatus.CREATED, created.getStatusCode(), created.getBody());
        String itemId = json(created).path("data").path("id").asText();

        // Search combines four filters; the item must match all of them.
        ResponseEntity<String> search = getJson(
                "/api/items?type=LOST&q=wallet&brand=fossil&categoryId=" + categoryId, finderToken);
        assertEquals(HttpStatus.OK, search.getStatusCode());
        boolean found = false;
        for (JsonNode node : json(search).path("data").path("content")) {
            if (itemId.equals(node.path("id").asText())) {
                found = true;
            }
        }
        assertTrue(found, "the reported item was not returned by the search");

        assertEquals(
                HttpStatus.CREATED,
                postJson("/api/saved-items/" + itemId, Map.of(), finderToken).getStatusCode());
        assertEquals(
                1, json(getJson("/api/saved-items", finderToken)).path("data").path("content").size());

        ResponseEntity<String> conversation =
                postJson("/api/conversations", Map.of("itemId", itemId), finderToken);
        assertEquals(HttpStatus.OK, conversation.getStatusCode());
        String conversationId = json(conversation).path("data").path("id").asText();

        ResponseEntity<String> message = postJson(
                "/api/conversations/" + conversationId + "/messages",
                Map.of("text", "Hi! I think I found your wallet."),
                finderToken);
        assertEquals(HttpStatus.CREATED, message.getStatusCode());

        JsonNode history = json(getJson("/api/conversations/" + conversationId + "/messages", ownerToken))
                .path("data")
                .path("content");
        assertEquals(1, history.size());
        assertEquals("Hi! I think I found your wallet.", history.get(0).path("text").asText());

        // Someone who is not a participant cannot read the thread.
        String stranger = registerUser("stranger@example.com").path("accessToken").asText();
        assertEquals(
                HttpStatus.FORBIDDEN,
                getJson("/api/conversations/" + conversationId + "/messages", stranger).getStatusCode());

        ResponseEntity<String> statusUpdate =
                patchJson("/api/items/" + itemId + "/status", Map.of("status", "RETURNED"), ownerToken);
        assertEquals(HttpStatus.OK, statusUpdate.getStatusCode());
        assertEquals("RETURNED", json(statusUpdate).path("data").path("status").asText());

        Map<String, Object> rating = new LinkedHashMap<>();
        rating.put("toUserId", ownerId);
        rating.put("itemId", itemId);
        rating.put("score", 4);
        rating.put("comment", "Quick pickup");

        ResponseEntity<String> ratingResponse = postJson("/api/ratings", rating, finderToken);
        assertEquals(HttpStatus.CREATED, ratingResponse.getStatusCode(), ratingResponse.getBody());

        JsonNode publicProfile =
                json(getJson("/api/users/" + ownerId, finderToken)).path("data");
        assertEquals(4.0, publicProfile.path("ratingAvg").asDouble());

        // Saving, messaging and rating each raised a notification for the owner.
        JsonNode notifications =
                json(getJson("/api/notifications", ownerToken)).path("data").path("content");
        List<String> types = new ArrayList<>();
        notifications.forEach(node -> types.add(node.path("type").asText()));
        assertTrue(types.contains("ITEM_SAVED"), "expected ITEM_SAVED in " + types);
        assertTrue(types.contains("NEW_MESSAGE"), "expected NEW_MESSAGE in " + types);
        assertTrue(types.contains("NEW_RATING"), "expected NEW_RATING in " + types);

        String firstNotificationId = notifications.get(0).path("id").asText();
        ResponseEntity<String> markRead =
                patchJson("/api/notifications/" + firstNotificationId + "/read", null, ownerToken);
        assertEquals(HttpStatus.OK, markRead.getStatusCode());
        assertTrue(json(markRead).path("data").path("isRead").asBoolean());
    }

    @Test
    @DisplayName("only the owner can modify or delete an item")
    void onlyOwnerCanModifyItem() {
        String ownerToken = registerUser("item-owner@example.com").path("accessToken").asText();
        String otherToken = registerUser("item-other@example.com").path("accessToken").asText();

        String categoryId = json(getJson("/api/categories", ownerToken))
                .path("data")
                .get(0)
                .path("id")
                .asText();

        Map<String, Object> newItem = new LinkedHashMap<>();
        newItem.put("type", "FOUND");
        newItem.put("name", "Set of keys");
        newItem.put("categoryId", categoryId);
        newItem.put("storageLocation", "Reception");

        ResponseEntity<String> created = postJson("/api/items", newItem, ownerToken);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        String itemId = json(created).path("data").path("id").asText();

        assertEquals(
                HttpStatus.FORBIDDEN,
                patchJson("/api/items/" + itemId + "/status", Map.of("status", "CLOSED"), otherToken)
                        .getStatusCode());
        assertEquals(
                HttpStatus.FORBIDDEN, deleteJson("/api/items/" + itemId, otherToken).getStatusCode());
    }

    private String categoryIdNamed(String token, String name) {
        for (JsonNode node : json(getJson("/api/categories", token)).path("data")) {
            if (name.equals(node.path("name").asText())) {
                return node.path("id").asText();
            }
        }
        throw new IllegalStateException("No seeded category named " + name);
    }
}
