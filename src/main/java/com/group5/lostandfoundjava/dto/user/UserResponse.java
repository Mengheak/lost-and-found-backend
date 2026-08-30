package com.group5.lostandfoundjava.dto.user;

import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * The full profile, shown to the owner of the account and to admins.
 *
 * <p>DTOs are Java {@code record}s: short, immutable, and — most importantly — they decide exactly
 * which entity fields reach the client. The {@code passwordHash} on {@link User} has no matching
 * field here, so it can never be leaked by accident.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String profilePhotoUrl,
        double ratingAvg,
        Role role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfilePhotoUrl(),
                user.getRatingAvg(),
                user.getRole(),
                user.getCreatedAt());
    }
}
