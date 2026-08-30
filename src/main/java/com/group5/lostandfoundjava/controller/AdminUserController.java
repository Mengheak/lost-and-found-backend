package com.group5.lostandfoundjava.controller;

import static com.group5.lostandfoundjava.config.OpenApiConfig.BEARER_SCHEME;

import com.group5.lostandfoundjava.common.ApiResponse;
import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.user.UpdateUserRoleRequest;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.service.AdminUserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User administration.
 *
 * <p>{@code @PreAuthorize} on the class applies to every method. The URL prefix is also locked down
 * in {@link com.group5.lostandfoundjava.config.SecurityConfig}; having both is deliberate, so
 * neither one alone is the single thing standing between a stranger and the admin area.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = BEARER_SCHEME)
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(
            summary = "List and search all users",
            description = "Requires the ADMIN role. Returns the full profile of every user, newest first. "
                    + "Pass `q` to filter by name or email.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Page of users"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin")
    })
    public ApiResponse<PageResponse<UserResponse>> list(
            @Parameter(description = "Free-text search across the name and email")
                    @RequestParam(required = false, name = "q")
                    String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ApiResponse.ok(adminUserService.list(keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get any user's full profile",
            description = "Requires the ADMIN role. Unlike the public profile endpoint, this includes the "
                    + "email and role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No user with that id")
    })
    public ApiResponse<UserResponse> get(@Parameter(description = "Id of the user") @PathVariable UUID id) {
        return ApiResponse.ok(adminUserService.get(id));
    }

    @PatchMapping("/{id}/role")
    @Operation(
            summary = "Promote or demote a user",
            description = "Requires the ADMIN role. Two guards protect the admin area from becoming "
                    + "unreachable: an admin cannot change their own role, and the last remaining admin cannot "
                    + "be demoted. The affected user keeps their old role until their next login or token "
                    + "refresh, because the role is baked into the access token when it is issued.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "The caller targeted their own account, or tried to demote the last admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "No user with that id")
    })
    public ApiResponse<UserResponse> updateRole(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID actingAdminId,
            @Parameter(description = "Id of the user whose role should change") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(actingAdminId, id, request.role()), "Role updated");
    }
}
