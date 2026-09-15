package com.group5.lostandfoundjava.mapper;

import com.group5.lostandfoundjava.dto.saveditem.SavedItemResponse;
import com.group5.lostandfoundjava.entity.Item;
import com.group5.lostandfoundjava.entity.SavedItem;
import com.group5.lostandfoundjava.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavedItemMapper {

    private final ItemMapper itemMapper;

    public SavedItem toEntity(User user, Item item) {
        return new SavedItem(user, item);
    }

    public SavedItemResponse toResponse(SavedItem savedItem) {
        if (savedItem == null) {
            return null;
        }
        return SavedItemResponse.builder()
                .id(savedItem.getId())
                .item(itemMapper.toResponse(savedItem.getItem()))
                // When the row was created is exactly when the user saved it.
                .savedAt(savedItem.getCreatedAt())
                .build();
    }

    public List<SavedItemResponse> toResponseList(List<SavedItem> savedItems) {
        if (savedItems == null) {
            return List.of();
        }
        List<SavedItemResponse> responses = new ArrayList<>(savedItems.size());
        savedItems.forEach(savedItem -> responses.add(toResponse(savedItem)));
        return responses;
    }
}
