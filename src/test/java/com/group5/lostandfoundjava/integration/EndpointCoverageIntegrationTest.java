package com.group5.lostandfoundjava.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Drives the endpoints no other integration test touches, and asserts on JSON field names
class EndpointCoverageIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    // --- categories ---

    @Test
    @DisplayName("category lifecycle: create, get one, rename, delete")
    void categoryLifecycle() {
        Admin admin = promoteToAdmin("cat-admin@example.com");
        String name = "Umbrellas " + UUID.randomUUID();

        ResponseEntity<String> created =
                postJson("/api/categories", Map.of("name", name, "iconUrl", "http://x/i.png"), admin.token);
        assertEquals(HttpStatus.CREATED, created.getStatusCode(), created.getBody());
        String id = json(created).path("data").path("id").asText();

        ResponseEntity<String> fetched = getJson("/api/categories/" + id, null);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        JsonNode body = json(fetched).path("data");
        assertEquals(name, body.path("name").asText());
        assertEquals("http://x/i.png", body.path("iconUrl").asText());

        String renamed = "Parasols " + UUID.randomUUID();
        ResponseEntity<String> updated =
                putJson("/api/categories/" + id, Map.of("name", renamed, "iconUrl", "http://x/j.png"), admin.token);
        assertEquals(HttpStatus.OK, updated.getStatusCode(), updated.getBody());
        assertEquals(renamed, json(updated).path("data").path("name").asText());
        assertEquals("http://x/j.png", json(updated).path("data").path("iconUrl").asText());

        assertEquals(HttpStatus.OK, deleteJson("/api/categories/" + id, admin.token).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, getJson("/api/categories/" + id, null).getStatusCode());
    }

    @Test
    @DisplayName("GET /api/categories/{id} returns 404 for an unknown id")
    void getUnknownCategory() {
        assertEquals(
                HttpStatus.NOT_FOUND,
                getJson("/api/categories/" + UUID.randomUUID(), null).getStatusCode());
    }

    // --- items ---

    @Test
    @DisplayName("GET /api/items/{id} returns the full item with its owner and category nested")
    void getSingleItem() {
        JsonNode user = registerUser("item-get@example.com", "Item Getter");
        String token = user.path("accessToken").asText();
        String ownerId = user.path("user").path("id").asText();
        String itemId = createItem(token, "Silver ring");

        ResponseEntity<String> response = getJson("/api/items/" + itemId, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode item = json(response).path("data");
        assertEquals("Silver ring", item.path("name").asText());
        assertEquals("LOST", item.path("type").asText());
        assertEquals("OPEN", item.path("status").asText());
        assertEquals(ownerId, item.path("owner").path("id").asText());
        assertEquals("Item Getter", item.path("owner").path("name").asText());
        assertFalse(item.path("category").path("name").asText().isBlank());
        assertTrue(item.path("photoUrls").isArray());
        assertFalse(item.path("createdAt").asText().isBlank());
        // A response must never leak the owner's private fields
        assertTrue(item.path("owner").path("email").isMissingNode());
    }

    @Test
    @DisplayName("PUT /api/items/{id} applies a partial update and leaves other fields alone")
    void updateItem() {
        String token = registerUser("item-put@example.com").path("accessToken").asText();
        String itemId = createItem(token, "Blue backpack");

        ResponseEntity<String> updated = putJson(
                "/api/items/" + itemId,
                Map.of("name", "Navy backpack", "brand", "Herschel", "photoUrls", List.of("http://x/1.png")),
                token);
        assertEquals(HttpStatus.OK, updated.getStatusCode(), updated.getBody());

        JsonNode item = json(updated).path("data");
        assertEquals("Navy backpack", item.path("name").asText());
        assertEquals("Herschel", item.path("brand").asText());
        assertEquals(1, item.path("photoUrls").size());
        assertEquals("LOST", item.path("type").asText());
    }

    @Test
    @DisplayName("PUT /api/items/{id} rejects a storage location on a LOST item")
    void updateItemRejectsWrongFieldForType() {
        String token = registerUser("item-put-bad@example.com").path("accessToken").asText();
        String itemId = createItem(token, "Wallet");

        ResponseEntity<String> response =
                putJson("/api/items/" + itemId, Map.of("storageLocation", "Front desk"), token);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(json(response).path("success").asBoolean());
    }

    @Test
    @DisplayName("PUT /api/items/{id} reports the validation messages on a bad body")
    void updateItemSurfacesValidationMessages() {
        String token = registerUser("item-put-invalid@example.com").path("accessToken").asText();
        String itemId = createItem(token, "Wallet");

        ResponseEntity<String> response =
                putJson("/api/items/" + itemId, Map.of("brand", "x".repeat(200)), token);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "brand length must be less than 100",
                json(response).path("data").path("brand").asText());
    }

    // --- users ---

    @Test
    @DisplayName("PUT /api/users/me updates the caller's own profile")
    void updateOwnProfile() {
        String token = registerUser("profile-put@example.com", "Before").path("accessToken").asText();

        ResponseEntity<String> updated = putJson(
                "/api/users/me",
                Map.of("name", "After", "phone", "012345678", "profilePhotoUrl", "http://x/me.png"),
                token);
        assertEquals(HttpStatus.OK, updated.getStatusCode(), updated.getBody());

        JsonNode me = json(updated).path("data");
        assertEquals("After", me.path("name").asText());
        assertEquals("012345678", me.path("phone").asText());
        assertEquals("http://x/me.png", me.path("profilePhotoUrl").asText());
        assertEquals("USER", me.path("role").asText());
        // The change really persisted rather than only being echoed back
        assertEquals("After", json(getJson("/api/users/me", token)).path("data").path("name").asText());
    }

    @Test
    @DisplayName("PUT /api/users/me rejects a blank name")
    void updateOwnProfileRejectsBlankName() {
        String token = registerUser("profile-blank@example.com").path("accessToken").asText();

        ResponseEntity<String> response = putJson("/api/users/me", Map.of("name", " "), token);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} returns the private view, unlike the public profile")
    void adminGetsSingleUser() {
        Admin admin = promoteToAdmin("admin-get-one@example.com");
        String targetId = registerUser("admin-target@example.com", "Target")
                .path("user")
                .path("id")
                .asText();

        ResponseEntity<String> response = getJson("/api/admin/users/" + targetId, admin.token);
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());

        JsonNode user = json(response).path("data");
        assertEquals("admin-target@example.com", user.path("email").asText());
        assertEquals("USER", user.path("role").asText());

        // The same user through the public endpoint exposes neither field
        JsonNode publicView = json(getJson("/api/users/" + targetId, null)).path("data");
        assertEquals("Target", publicView.path("name").asText());
        assertTrue(publicView.path("email").isMissingNode());
        assertTrue(publicView.path("role").isMissingNode());
        assertFalse(publicView.path("memberSince").asText().isBlank());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} is refused to a regular user")
    void adminGetSingleUserIsAdminOnly() {
        String token = registerUser("not-an-admin@example.com").path("accessToken").asText();

        assertEquals(
                HttpStatus.FORBIDDEN,
                getJson("/api/admin/users/" + UUID.randomUUID(), token).getStatusCode());
    }

    // --- chat ---

    @Test
    @DisplayName("chat: start a conversation, send a message over REST, list both")
    void chatOverRest() {
        JsonNode owner = registerUser("chat-owner@example.com", "Owner");
        JsonNode finder = registerUser("chat-finder@example.com", "Finder");
        String ownerToken = owner.path("accessToken").asText();
        String finderToken = finder.path("accessToken").asText();
        String finderId = finder.path("user").path("id").asText();

        String itemId = createItem(ownerToken, "Lost passport");

        ResponseEntity<String> started =
                postJson("/api/conversations", Map.of("itemId", itemId), finderToken);
        assertEquals(HttpStatus.OK, started.getStatusCode(), started.getBody());
        JsonNode conversation = json(started).path("data");
        String conversationId = conversation.path("id").asText();
        assertEquals("Lost passport", conversation.path("item").path("name").asText());
        assertEquals("Finder", conversation.path("userA").path("name").asText());
        assertEquals("Owner", conversation.path("userB").path("name").asText());

        ResponseEntity<String> sent = postJson(
                "/api/conversations/" + conversationId + "/messages",
                Map.of("text", "  I think I found it  "),
                finderToken);
        assertEquals(HttpStatus.CREATED, sent.getStatusCode(), sent.getBody());
        JsonNode message = json(sent).path("data");
        assertEquals("I think I found it", message.path("text").asText(), "the mapper should trim the text");
        assertEquals(conversationId, message.path("conversationId").asText());
        assertEquals(finderId, message.path("senderId").asText());

        ResponseEntity<String> listed =
                getJson("/api/conversations/" + conversationId + "/messages", ownerToken);
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        JsonNode page = json(listed).path("data");
        assertEquals(1, page.path("totalElements").asInt());
        assertEquals("I think I found it", page.path("content").get(0).path("text").asText());

        ResponseEntity<String> conversations = getJson("/api/conversations", finderToken);
        assertEquals(HttpStatus.OK, conversations.getStatusCode());
        assertEquals(1, json(conversations).path("data").path("totalElements").asInt());
    }

    @Test
    @DisplayName("a message with neither text nor image is refused")
    void emptyMessageIsRejected() {
        JsonNode owner = registerUser("chat-empty-owner@example.com");
        String finderToken = registerUser("chat-empty-finder@example.com")
                .path("accessToken")
                .asText();
        String itemId = createItem(owner.path("accessToken").asText(), "Keys");

        String conversationId = json(postJson("/api/conversations", Map.of("itemId", itemId), finderToken))
                .path("data")
                .path("id")
                .asText();

        ResponseEntity<String> response = postJson(
                "/api/conversations/" + conversationId + "/messages", Map.of("text", "   "), finderToken);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("an outsider cannot read another pair's messages")
    void outsiderCannotReadMessages() {
        JsonNode owner = registerUser("chat-priv-owner@example.com");
        String finderToken = registerUser("chat-priv-finder@example.com")
                .path("accessToken")
                .asText();
        String outsiderToken = registerUser("chat-priv-outsider@example.com")
                .path("accessToken")
                .asText();
        String itemId = createItem(owner.path("accessToken").asText(), "Headphones");

        String conversationId = json(postJson("/api/conversations", Map.of("itemId", itemId), finderToken))
                .path("data")
                .path("id")
                .asText();

        assertEquals(
                HttpStatus.FORBIDDEN,
                getJson("/api/conversations/" + conversationId + "/messages", outsiderToken)
                        .getStatusCode());
    }

    // --- saved items, notifications, ratings ---

    @Test
    @DisplayName("DELETE /api/saved-items/{itemId} removes the item from the shortlist")
    void unsaveItem() {
        String ownerToken = registerUser("save-owner@example.com").path("accessToken").asText();
        String saverToken = registerUser("save-saver@example.com").path("accessToken").asText();
        String itemId = createItem(ownerToken, "Sunglasses");

        assertEquals(
                HttpStatus.CREATED,
                postJson("/api/saved-items/" + itemId, null, saverToken).getStatusCode());
        assertEquals(
                1,
                json(getJson("/api/saved-items", saverToken))
                        .path("data")
                        .path("totalElements")
                        .asInt());

        assertEquals(
                HttpStatus.OK,
                deleteJson("/api/saved-items/" + itemId, saverToken).getStatusCode());
        assertEquals(
                0,
                json(getJson("/api/saved-items", saverToken))
                        .path("data")
                        .path("totalElements")
                        .asInt());

        // Removing it twice is a 404, not a silent success
        assertEquals(
                HttpStatus.NOT_FOUND,
                deleteJson("/api/saved-items/" + itemId, saverToken).getStatusCode());
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all marks the whole feed read")
    void markAllNotificationsRead() {
        String ownerToken = registerUser("notify-owner@example.com").path("accessToken").asText();
        String saverToken = registerUser("notify-saver@example.com").path("accessToken").asText();

        // Two saves by someone else raise two notifications for the owner
        postJson("/api/saved-items/" + createItem(ownerToken, "Laptop"), null, saverToken);
        postJson("/api/saved-items/" + createItem(ownerToken, "Charger"), null, saverToken);

        JsonNode feed = json(getJson("/api/notifications", ownerToken)).path("data");
        assertEquals(2, feed.path("totalElements").asInt());
        // The boolean must serialise as isRead, not as read
        JsonNode first = feed.path("content").get(0);
        assertTrue(first.has("isRead"), "expected an isRead field, got: " + first);
        assertFalse(first.path("isRead").asBoolean());

        ResponseEntity<String> readAll = patchJson("/api/notifications/read-all", null, ownerToken);
        assertEquals(HttpStatus.OK, readAll.getStatusCode(), readAll.getBody());
        assertEquals(2, json(readAll).path("data").path("updated").asInt());

        JsonNode after = json(getJson("/api/notifications", ownerToken)).path("data");
        after.path("content").forEach(node -> assertTrue(node.path("isRead").asBoolean()));
    }

    @Test
    @DisplayName("GET /api/ratings/user/{userId} lists the ratings a user received")
    void listRatingsForUser() {
        JsonNode owner = registerUser("rating-owner@example.com", "Rated One");
        JsonNode rater = registerUser("rating-rater@example.com", "The Rater");
        String ownerId = owner.path("user").path("id").asText();
        String itemId = createItem(owner.path("accessToken").asText(), "Bicycle");

        Map<String, Object> rating = new LinkedHashMap<>();
        rating.put("toUserId", ownerId);
        rating.put("itemId", itemId);
        rating.put("score", 4);
        rating.put("comment", "Very helpful");
        assertEquals(
                HttpStatus.CREATED,
                postJson("/api/ratings", rating, rater.path("accessToken").asText())
                        .getStatusCode());

        ResponseEntity<String> listed = getJson("/api/ratings/user/" + ownerId, null);
        assertEquals(HttpStatus.OK, listed.getStatusCode());

        JsonNode entry = json(listed).path("data").path("content").get(0);
        assertEquals(4, entry.path("score").asInt());
        assertEquals("Very helpful", entry.path("comment").asText());
        assertEquals(ownerId, entry.path("toUserId").asText());
        assertEquals(itemId, entry.path("itemId").asText());
        assertEquals("The Rater", entry.path("fromUser").path("name").asText());

        // The cached average on the profile was updated by the rating
        assertEquals(
                4.0,
                json(getJson("/api/users/" + ownerId, null))
                        .path("data")
                        .path("ratingAvg")
                        .asDouble());
    }

    @Test
    @DisplayName("GET /api/ratings/user/{userId} is 404 for an unknown user")
    void listRatingsForUnknownUser() {
        assertEquals(
                HttpStatus.NOT_FOUND,
                getJson("/api/ratings/user/" + UUID.randomUUID(), null).getStatusCode());
    }

    // --- helpers ---

    private record Admin(String id, String token) {}

    // Register a user, promote them in the database, log in for a token carrying ADMIN
    private Admin promoteToAdmin(String email) {
        JsonNode registered = registerUser(email);
        String id = registered.path("user").path("id").asText();

        User user = userRepository.findById(UUID.fromString(id)).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("email", email);
        credentials.put("password", "password123");

        ResponseEntity<String> login = postJson("/api/auth/login", credentials, null);
        assertEquals(HttpStatus.OK, login.getStatusCode(), "admin login failed: " + login.getBody());
        return new Admin(id, json(login).path("data").path("accessToken").asText());
    }

    // Report a LOST item in the first seeded category, return its id
    private String createItem(String token, String name) {
        String categoryId = json(getJson("/api/categories", null))
                .path("data")
                .get(0)
                .path("id")
                .asText();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "LOST");
        body.put("name", name);
        body.put("categoryId", categoryId);

        ResponseEntity<String> created = postJson("/api/items", body, token);
        assertEquals(HttpStatus.CREATED, created.getStatusCode(), "item creation failed: " + created.getBody());
        return json(created).path("data").path("id").asText();
    }
}
