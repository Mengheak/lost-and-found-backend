package com.group5.lostandfoundjava.dto.chat;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class StartConversationRequest {

    private UUID itemId;

    // Optional: left out, the conversation is started with whoever reported the item
    private UUID otherUserId;
}
