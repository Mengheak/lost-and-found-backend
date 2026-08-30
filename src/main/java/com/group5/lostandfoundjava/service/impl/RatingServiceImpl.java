package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.common.exception.BadRequestException;
import com.group5.lostandfoundjava.common.exception.ConflictException;
import com.group5.lostandfoundjava.common.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.rating.RatingResponse;
import com.group5.lostandfoundjava.dto.rating.SubmitRatingRequest;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.Rating;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.RatingRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.NotificationService;
import com.group5.lostandfoundjava.service.RatingService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reputation left after an item changes hands. */
@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    public RatingServiceImpl(
            RatingRepository ratingRepository,
            UserRepository userRepository,
            ItemRepository itemRepository,
            NotificationService notificationService) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public RatingResponse submit(UUID fromUserId, SubmitRatingRequest request) {
        if (fromUserId.equals(request.toUserId())) {
            throw new BadRequestException("You cannot rate yourself");
        }

        User fromUser = userRepository.findById(fromUserId).orElseThrow(() -> new NotFoundException("User not found"));
        User toUser = userRepository
                .findById(request.toUserId())
                .orElseThrow(() -> new NotFoundException("Rated user not found"));
        Item item =
                itemRepository.findById(request.itemId()).orElseThrow(() -> new NotFoundException("Item not found"));

        if (ratingRepository.existsByFromUserIdAndToUserIdAndItemId(
                fromUserId, request.toUserId(), request.itemId())) {
            throw new ConflictException("You have already rated this user for this item");
        }

        Rating rating =
                ratingRepository.save(new Rating(fromUser, toUser, item, request.score(), request.comment()));

        // The average is cached on the user so profile pages do not have to aggregate on every read.
        toUser.setRatingAvg(ratingRepository.averageScoreFor(toUser.getId()));
        userRepository.save(toUser);

        notificationService.notify(
                toUser,
                NotificationType.NEW_RATING,
                fromUser.getName() + " rated you " + request.score() + "/5 for \"" + item.getName() + "\"");

        return RatingResponse.from(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RatingResponse> listForUser(UUID userId, Pageable pageable) {
        // Distinguishes "this user has no ratings yet" (empty page) from "no such user" (404).
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        return PageResponse.from(ratingRepository.findByToUserId(userId, pageable).map(RatingResponse::from));
    }
}
