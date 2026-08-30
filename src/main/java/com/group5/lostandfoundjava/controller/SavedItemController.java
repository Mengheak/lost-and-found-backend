package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.saveditem.SavedItemResponse;
import com.group5.lostandfoundjava.service.SavedItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The caller's personal shortlist. Every endpoint here works on the caller's own list only. */
@RestController
@RequestMapping("/api/saved-items")
@Tag(name = "Saved Items")
@SecurityRequirement(name = BEARER_SCHEME)
public class SavedItemController {

    private final SavedItemService savedItemService;

    public SavedItemController(SavedItemService savedItemService) {
        this.savedItemService = savedItemService;
    }

    @PostMapping("/{itemId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Save an item to the caller's shortlist",
            description = "Idempotent: saving an item that is already saved returns the existing record "
                    + "instead of failing. Saving someone else's item notifies its owner.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Item saved, or already was"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No item with that id")
    })
    public ApiResponse<SavedItemResponse> save(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the item to save") @PathVariable UUID itemId) {
        return ApiResponse.ok(savedItemService.save(userId, itemId), "Item saved");
    }

    @DeleteMapping("/{itemId}")
    @Operation(
            summary = "Remove an item from the caller's shortlist",
            description = "Identified by the item id rather than the saved-item id, so the client does "
                    + "not need to track a separate identifier.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Item removed from the saved list"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "That item is not in the caller's saved list")
    })
    public ApiResponse<Void> unsave(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the item to remove") @PathVariable UUID itemId) {
        savedItemService.unsave(userId, itemId);
        return ApiResponse.message("Item removed from saved list");
    }

    @GetMapping
    @Operation(
            summary = "List the caller's saved items",
            description = "Paged, most recently saved first. Each entry embeds a compact view of the item.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Page of saved items"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<PageResponse<SavedItemResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(savedItemService.list(userId, pageable));
    }
}
