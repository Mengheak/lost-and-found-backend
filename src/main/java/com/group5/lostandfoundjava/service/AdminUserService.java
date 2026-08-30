package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.entity.enums.Role;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** User administration. Every method here is reachable only by an admin. */
public interface AdminUserService {

    PageResponse<UserResponse> list(String keyword, Pageable pageable);

    UserResponse get(UUID userId);

    UserResponse updateRole(UUID actingAdminId, UUID userId, Role role);
}
