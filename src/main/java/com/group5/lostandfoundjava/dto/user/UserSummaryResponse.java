package com.group5.lostandfoundjava.dto.user;

import com.group5.lostandfoundjava.entity.User;
import java.util.UUID;

/** The compact view embedded inside other responses, such as the reporter of an item. */
public record UserSummaryResponse(UUID id, String name, String profilePhotoUrl, double ratingAvg) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(), user.getName(), user.getProfilePhotoUrl(), user.getRatingAvg());
    }
}
