package com.group5.lostandfoundjava.service;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.dto.rating.RatingResponse;
import com.group5.lostandfoundjava.dto.rating.SubmitRatingRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Reputation left after an item changes hands. */
public interface RatingService {

    RatingResponse submit(UUID fromUserId, SubmitRatingRequest request);

    PageResponse<RatingResponse> listForUser(UUID userId, Pageable pageable);
}
