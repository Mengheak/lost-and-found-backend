package com.group5.lostandfoundjava.dto.chat;

import com.group5.lostandfoundjava.dto.item.ItemSummaryResponse;
import com.group5.lostandfoundjava.dto.user.UserSummaryResponse;
import com.group5.lostandfoundjava.entity.Conversation;
import java.time.Instant;
import java.util.UUID;

/** A chat thread: which item it is about and who the two participants are. */
public record ConversationResponse(
        UUID id,
        ItemSummaryResponse item,
        UserSummaryResponse userA,
        UserSummaryResponse userB,
        Instant createdAt) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                ItemSummaryResponse.from(conversation.getItem()),
                UserSummaryResponse.from(conversation.getUserA()),
                UserSummaryResponse.from(conversation.getUserB()),
                conversation.getCreatedAt());
    }
}
