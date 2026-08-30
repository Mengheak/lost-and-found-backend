package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.item.CreateItemRequest;
import com.group5.lostandfoundjava.dto.item.ItemResponse;
import com.group5.lostandfoundjava.dto.item.ItemSearchFilter;
import com.group5.lostandfoundjava.dto.item.UpdateItemRequest;
import com.group5.lostandfoundjava.dto.item.UpdateItemStatusRequest;
import com.group5.lostandfoundjava.entity.enums.ItemStatus;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import com.group5.lostandfoundjava.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reporting, searching and managing lost or found items. */
@RestController
@RequestMapping("/api/items")
@Tag(name = "Items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Report a lost or found item",
            description = "Creates an item owned by the signed-in user. `type` decides whether this is "
                    + "something the reporter lost or something they found.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Item created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "The referenced category does not exist")
    })
    public ApiResponse<ItemResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateItemRequest request) {
        return ApiResponse.ok(itemService.create(userId, request), "Item reported");
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a single item",
            description = "Public endpoint. Includes a compact profile of the reporter so the client can "
                    + "offer to start a conversation.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No item with that id")
    })
    public ApiResponse<ItemResponse> get(@Parameter(description = "Id of the item") @PathVariable UUID id) {
        return ApiResponse.ok(itemService.get(id));
    }

    /**
     * The eight filters arrive as separate query parameters and are bundled into one
     * {@link ItemSearchFilter} before being handed to the service.
     */
    @GetMapping
    @Operation(
            summary = "Search items",
            description = "Public, paged search. All filters are optional and combine with AND; omitting "
                    + "everything returns the newest items first. Use the `page`, `size` and `sort` query "
                    + "parameters for paging.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Page of matching items"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Unparsable filter value or unknown sort property")
    })
    public ApiResponse<PageResponse<ItemResponse>> search(
            @Parameter(description = "Restrict to items that were LOST or FOUND") @RequestParam(required = false)
                    ItemType type,
            @Parameter(description = "Restrict to a lifecycle status, e.g. OPEN or RETURNED")
                    @RequestParam(required = false)
                    ItemStatus status,
            @Parameter(description = "Restrict to a single category") @RequestParam(required = false)
                    UUID categoryId,
            @Parameter(description = "Free-text search across the title and description")
                    @RequestParam(required = false, name = "q")
                    String keyword,
            @Parameter(description = "Brand of the item, matched case-insensitively")
                    @RequestParam(required = false)
                    String brand,
            @Parameter(description = "Colour of the item, matched case-insensitively")
                    @RequestParam(required = false)
                    String color,
            @Parameter(description = "Only items dated at or after this ISO-8601 instant, e.g. 2026-01-31T00:00:00Z")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant dateFrom,
            @Parameter(description = "Only items dated at or before this ISO-8601 instant")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant dateTo,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {

        ItemSearchFilter filter =
                new ItemSearchFilter(type, status, categoryId, keyword, brand, color, dateFrom, dateTo);
        return ApiResponse.ok(itemService.search(filter, pageable));
    }

    @GetMapping("/my")
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "List the signed-in user's own items",
            description = "Paged list of everything the caller has reported, newest first, regardless of status.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Page of the caller's items"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<PageResponse<ItemResponse>> listMine(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(itemService.listOwn(userId, pageable));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Update an item",
            description = "Only the user who reported the item may edit it. Fields left null are kept as they are.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "The item belongs to another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No item with that id")
    })
    public ApiResponse<ItemResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the item to update") @PathVariable UUID id,
            @Valid @RequestBody UpdateItemRequest request) {
        return ApiResponse.ok(itemService.update(userId, id, request), "Item updated");
    }

    @PatchMapping("/{id}/status")
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Change an item's status",
            description = "Used to close the loop, typically by marking an item as RETURNED once it has "
                    + "been handed back. Only the reporter may change the status.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Unknown status value"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "The item belongs to another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No item with that id")
    })
    public ApiResponse<ItemResponse> updateStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the item") @PathVariable UUID id,
            @Valid @RequestBody UpdateItemStatusRequest request) {
        return ApiResponse.ok(itemService.updateStatus(userId, id, request.status()), "Item status updated");
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Delete an item",
            description = "Only the user who reported the item may delete it. Returns an envelope with a "
                    + "confirmation message and no data.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "The item belongs to another user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No item with that id")
    })
    public ApiResponse<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Id of the item to delete") @PathVariable UUID id) {
        itemService.delete(userId, id);
        return ApiResponse.message("Item deleted");
    }
}
