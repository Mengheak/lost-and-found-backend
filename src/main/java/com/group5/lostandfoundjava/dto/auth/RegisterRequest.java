package com.group5.lostandfoundjava.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/auth/register}.
 *
 * <p>The annotations are checked by Spring before the controller method runs, thanks to the
 * {@code @Valid} on the parameter. A failure never reaches the service — it comes back as a 400
 * listing every bad field.
 *
 * <p>The 72-character maximum on the password is not arbitrary: bcrypt ignores anything beyond it.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @NotBlank @Size(min = 8, max = 72) String password) {}
