package com.group5.lostandfoundjava.dto.chat;

import com.group5.lostandfoundjava.dto.item.ItemSummaryResponse;
import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// A chat thread between two users about one item
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;

    private ItemSummaryResponse item;

    private UserSummaryResponse userA;

    private UserSummaryResponse userB;

    private Instant createdAt;
}
