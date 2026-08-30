package com.group5.lostandfoundjava.dto.user;

import com.group5.lostandfoundjava.entity.User;
import java.time.Instant;
import java.util.UUID;

/** What other users are allowed to see: no email, no phone number, no role. */
public record PublicUserResponse(
        UUID id, String name, String profilePhotoUrl, double ratingAvg, Instant memberSince) {

    public static PublicUserResponse from(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getName(),
                user.getProfilePhotoUrl(),
                user.getRatingAvg(),
                user.getCreatedAt());
    }
}
