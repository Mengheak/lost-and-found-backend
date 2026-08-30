package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.common.exception.BadRequestException;
import com.group5.lostandfoundjava.common.exception.ConflictException;
import com.group5.lostandfoundjava.dto.rating.RatingResponse;
import com.group5.lostandfoundjava.dto.rating.SubmitRatingRequest;
import com.group5.lostandfoundjava.entity.Category;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.ItemType;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.RatingRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rating rules: no self-rating, no duplicates, and the average is kept up to date. */
class RatingServiceImplTest {

    private final RatingRepository ratingRepository = mock(RatingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final RatingServiceImpl service =
            new RatingServiceImpl(ratingRepository, userRepository, itemRepository, notificationService);

    private final User rater = new User("Rater", "rater@example.com", null, "hash", Role.USER);
    private final User rated = new User("Rated", "rated@example.com", null, "hash", Role.USER);
    private final Item item = new Item(rated, new Category("Keys", null), ItemType.FOUND, "Car keys");

    @Test
    @DisplayName("submit persists the rating, recalculates rating_avg and notifies the rated user")
    void submitPersistsAndRecalculatesAverage() {
        when(userRepository.findById(rater.getId())).thenReturn(Optional.of(rater));
        when(userRepository.findById(rated.getId())).thenReturn(Optional.of(rated));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(ratingRepository.existsByFromUserIdAndToUserIdAndItemId(rater.getId(), rated.getId(), item.getId()))
                .thenReturn(false);
        when(ratingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ratingRepository.averageScoreFor(rated.getId())).thenReturn(4.5);
        when(userRepository.save(rated)).thenReturn(rated);

        RatingResponse response =
                service.submit(rater.getId(), new SubmitRatingRequest(rated.getId(), item.getId(), 5, "Great!"));

        assertEquals(5, response.score());
        assertEquals(4.5, rated.getRatingAvg());
        verify(userRepository).save(rated);
        verify(notificationService).notify(eq(rated), eq(NotificationType.NEW_RATING), any());
    }

    @Test
    @DisplayName("submit rejects rating yourself")
    void submitRejectsSelfRating() {
        assertThrows(
                BadRequestException.class,
                () -> service.submit(
                        rater.getId(), new SubmitRatingRequest(rater.getId(), item.getId(), 5, null)));
    }

    @Test
    @DisplayName("submit rejects a duplicate rating for the same user and item")
    void submitRejectsDuplicate() {
        when(userRepository.findById(rater.getId())).thenReturn(Optional.of(rater));
        when(userRepository.findById(rated.getId())).thenReturn(Optional.of(rated));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(ratingRepository.existsByFromUserIdAndToUserIdAndItemId(rater.getId(), rated.getId(), item.getId()))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.submit(
                        rater.getId(), new SubmitRatingRequest(rated.getId(), item.getId(), 3, null)));
    }
}
