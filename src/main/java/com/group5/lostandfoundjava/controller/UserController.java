package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.dto.user.PublicUserResponse;
import com.group5.lostandfoundjava.dto.user.UpdateProfileRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Own profile and public profiles of other users. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code @AuthenticationPrincipal} hands us the id that
     * {@link com.group5.lostandfoundjava.security.JwtAuthenticationFilter} put in the security
     * context. Taking it from the token instead of from a path parameter is what makes this
     * endpoint safe: a caller can only ever read their own profile.
     */
    @GetMapping("/me")
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Get the signed-in user's own profile",
            description = "Returns the full profile, including the email and role that are hidden from "
                    + "the public view. The user is taken from the access token, not from a path parameter.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<UserResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    @SecurityRequirement(name = BEARER_SCHEME)
    @Operation(
            summary = "Update the signed-in user's own profile",
            description = "Updates the editable profile fields. The email, password and role cannot be "
                    + "changed here.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<UserResponse> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(userId, request), "Profile updated");
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get another user's public profile",
            description = "Public endpoint used to show who reported an item. Returns only the fields "
                    + "safe to expose to other users, such as the display name and the average rating.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Public profile returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No user with that id")
    })
    public ApiResponse<PublicUserResponse> getPublicProfile(
            @Parameter(description = "Id of the user to look up") @PathVariable UUID id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }
}
