package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.notification.NotificationResponse;
import com.group5.lostandfoundjava.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's in-app notification feed. */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
@SecurityRequirement(name = BEARER_SCHEME)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(
            summary = "List the caller's notifications",
            description = "Paged feed, newest first. Notifications are raised when someone saves the "
                    + "caller's item, sends them a message, or rates them.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Page of notifications"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(notificationService.list(userId, pageable));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Mark one notification as read",
            description = "Only the notification's own recipient may mark it read.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Notification marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "The notification belongs to another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No notification with that id")
    })
    public ApiResponse<NotificationResponse> markRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the notification") @PathVariable UUID id) {
        return ApiResponse.ok(notificationService.markRead(userId, id), "Notification marked as read");
    }

    @PatchMapping("/read-all")
    @Operation(
            summary = "Mark every unread notification as read",
            description = "Returns `{ \"updated\": n }` in `data`, where `n` is how many rows were changed.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "All notifications marked as read"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<Map<String, Integer>> markAllRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        int updated = notificationService.markAllRead(userId);
        return ApiResponse.ok(Map.of("updated", updated), "All notifications marked as read");
    }
}
