package com.group5.lostandfoundjava.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Body of {@code POST /api/ratings}. */
public record SubmitRatingRequest(
        UUID toUserId, UUID itemId, @Min(1) @Max(5) int score, @Size(max = 2000) String comment) {}
