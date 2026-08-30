package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.chat.MessageResponse;
import com.group5.lostandfoundjava.dto.chat.SendMessageRequest;
import com.group5.lostandfoundjava.service.MessageService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Messages inside a conversation.
 *
 * <p>This is the plain HTTP way to chat, used for loading history and as a fallback when a
 * WebSocket is not available. Either way the message ends up broadcast to the live thread.
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@Tag(name = "Messages")
@SecurityRequirement(name = BEARER_SCHEME)
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Send a message in a conversation",
            description = "A message must carry text, an image, or both. The saved message is also "
                    + "broadcast to WebSocket subscribers of this conversation, so a message sent over REST "
                    + "and one sent over STOMP behave identically. The recipient is notified.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Message stored and broadcast"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "The message has neither text nor an image"),
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
    public ApiResponse<MessageResponse> send(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the conversation to post into") @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(messageService.send(userId, conversationId, request), "Message sent");
    }

    @GetMapping
    @Operation(
            summary = "List the messages in a conversation",
            description = "Paged, newest first, so the first page is what a chat screen shows on open. "
                    + "Readable only by the two participants.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of messages"),
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
    public ApiResponse<PageResponse<MessageResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the conversation to read") @PathVariable UUID conversationId,
            @ParameterObject @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(messageService.list(userId, conversationId, pageable));
    }
}
