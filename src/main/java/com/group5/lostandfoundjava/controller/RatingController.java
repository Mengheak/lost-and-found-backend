package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.rating.RatingResponse;
import com.group5.lostandfoundjava.dto.rating.SubmitRatingRequest;
import com.group5.lostandfoundjava.service.RatingService;
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

/** Reputation left after an item is returned. */
@RestController
@RequestMapping("/api/ratings")
@Tag(name = "Ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Rate another user for an item",
            description = "Left after an item changes hands, to build reputation. A user cannot rate "
                    + "themselves, and may rate a given user only once per item. Submitting a rating updates "
                    + "the rated user's average and notifies them.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Rating stored and the rated user's average updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed, or the caller tried to rate themselves"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No such item or rated user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "The caller has already rated that user for that item")
    })
    public ApiResponse<RatingResponse> submit(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SubmitRatingRequest request) {
        return ApiResponse.ok(ratingService.submit(userId, request), "Rating submitted");
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "List the ratings a user has received",
            description = "Public endpoint, so a profile screen can show reviews without signing in. "
                    + "Paged, newest first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Page of ratings received by that user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No user with that id")
    })
    public ApiResponse<PageResponse<RatingResponse>> listForUser(
            @Parameter(description = "Id of the user whose received ratings to list") @PathVariable UUID userId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(ratingService.listForUser(userId, pageable));
    }
}
