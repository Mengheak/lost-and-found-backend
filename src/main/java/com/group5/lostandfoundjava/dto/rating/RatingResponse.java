package com.group5.lostandfoundjava.dto.rating;

import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import com.group5.lostandfoundjava.entity.Rating;
import java.time.Instant;
import java.util.UUID;

/** A rating as shown on a profile page: who left it, for which item, and what they said. */
public record    RatingResponse(
        UUID id,
        UserSummaryResponse fromUser,
        UUID toUserId,
        UUID itemId,
        int score,
        String comment,
        Instant createdAt) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                UserSummaryResponse.from(rating.getFromUser()),
                rating.getToUser().getId(),
                rating.getItem().getId(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt());
    }
}
