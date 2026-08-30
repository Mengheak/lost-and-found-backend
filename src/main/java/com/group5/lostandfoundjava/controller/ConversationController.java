package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.chat.ConversationResponse;
import com.group5.lostandfoundjava.dto.chat.StartConversationRequest;
import com.group5.lostandfoundjava.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chat threads between two users about an item. */
@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations")
@SecurityRequirement(name = BEARER_SCHEME)
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /** Returns 200 rather than 201, because it may well have returned an existing thread. */
    @PostMapping
    @Operation(
            summary = "Start a conversation about an item, or get the existing one",
            description = "Idempotent for a given (item, caller, other user) triple: calling it twice "
                    + "returns the same thread rather than creating a duplicate. `otherUserId` defaults to the "
                    + "item's owner when omitted, which is the usual case.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Conversation created or already existed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed, or the caller is the other participant"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No item or no other user with that id")
    })
    public ApiResponse<ConversationResponse> startOrGet(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StartConversationRequest request) {
        return ApiResponse.ok(conversationService.startOrGet(userId, request), "Conversation ready");
    }

    @GetMapping
    @Operation(
            summary = "List the caller's conversations",
            description = "Paged list of every thread the caller participates in, newest first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Page of conversations"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<PageResponse<ConversationResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(conversationService.listForUser(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a single conversation",
            description = "Readable only by the two participants; anyone else is refused.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Conversation returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not a participant of this conversation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No conversation with that id")
    })
    public ApiResponse<ConversationResponse> get(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the conversation") @PathVariable UUID id) {
        return ApiResponse.ok(conversationService.getForUser(id, userId));
    }
}
