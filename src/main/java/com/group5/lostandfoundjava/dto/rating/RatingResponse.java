package com.group5.lostandfoundjava.dto.rating;

import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A rating as shown on a profile page: who left it, for which item, and what they said. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private UUID id;

    private UserSummaryResponse fromUser;

    private UUID toUserId;

    private UUID itemId;

    private int score;

    private String comment;

    private Instant createdAt;
}
