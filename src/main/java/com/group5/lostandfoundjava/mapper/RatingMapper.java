package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.rating.RatingResponse;
import com.group5.lostandfoundjava.dto.rating.SubmitRatingRequest;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.Rating;
import com.group5.lostandfoundjava.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingMapper {

    private final UserMapper userMapper;

    public Rating toEntity(SubmitRatingRequest request, User fromUser, User toUser, Item item) {
        if (request == null) {
            return null;
        }
        return new Rating(fromUser, toUser, item, request.getScore(), request.getComment());
    }

    public RatingResponse toResponse(Rating rating) {
        if (rating == null) {
            return null;
        }
        return RatingResponse.builder()
                .id(rating.getId())
                .fromUser(userMapper.toSummaryResponse(rating.getFromUser()))
                .toUserId(rating.getToUser().getId())
                .itemId(rating.getItem().getId())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    public List<RatingResponse> toResponseList(List<Rating> ratings) {
        if (ratings == null) {
            return List.of();
        }
        List<RatingResponse> responses = new ArrayList<>(ratings.size());
        ratings.forEach(rating -> responses.add(toResponse(rating)));
        return responses;
    }
}
