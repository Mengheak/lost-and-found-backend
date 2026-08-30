package com.group5.lostandfoundjava.service.impl;

import com.group5.lostandfoundjava.common.PageResponse;
import com.group5.lostandfoundjava.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.saveditem.SavedItemResponse;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.SavedItem;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import com.group5.lostandfoundjava.repository.ItemRepository;
import com.group5.lostandfoundjava.repository.SavedItemRepository;
import com.group5.lostandfoundjava.repository.UserRepository;
import com.group5.lostandfoundjava.service.NotificationService;
import com.group5.lostandfoundjava.service.SavedItemService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's personal shortlist of items. */
@Service
public class SavedItemServiceImpl implements SavedItemService {

    private final SavedItemRepository savedItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SavedItemServiceImpl(
            SavedItemRepository savedItemRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.savedItemRepository = savedItemRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SavedItemResponse save(UUID userId, UUID itemId) {
        // Idempotent: a double tap on the bookmark button must not fail or create a second row.
        Optional<SavedItem> existing = savedItemRepository.findByUserIdAndItemId(userId, itemId);
        if (existing.isPresent()) {
            return SavedItemResponse.from(existing.get());
        }

        Item item = itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        SavedItem saved = savedItemRepository.save(new SavedItem(user, item));

        // Interest in an item is worth telling its owner about — but not when they saved their own.
        if (!item.getUser().getId().equals(userId)) {
            notificationService.notify(
                    item.getUser(),
                    NotificationType.ITEM_SAVED,
                    user.getName() + " saved your item \"" + item.getName() + "\"");
        }

        return SavedItemResponse.from(saved);
    }

    @Override
    @Transactional
    public void unsave(UUID userId, UUID itemId) {
        SavedItem saved = savedItemRepository
                .findByUserIdAndItemId(userId, itemId)
                .orElseThrow(() -> new NotFoundException("Item is not in your saved list"));
        savedItemRepository.delete(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SavedItemResponse> list(UUID userId, Pageable pageable) {
        return PageResponse.from(savedItemRepository.findByUserId(userId, pageable).map(SavedItemResponse::from));
    }
}
