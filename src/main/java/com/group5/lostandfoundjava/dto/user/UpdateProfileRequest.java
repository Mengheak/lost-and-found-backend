package com.group5.lostandfoundjava.dto.user;

import jakarta.validation.constraints.Size;

/**
 * Partial update of the caller's own profile: any field left out (or sent as {@code null}) keeps
 * its current value. Email, password and role deliberately cannot be changed here.
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 255) String name,
        @Size(max = 50) String phone,
        @Size(max = 1024) String profilePhotoUrl) {}
