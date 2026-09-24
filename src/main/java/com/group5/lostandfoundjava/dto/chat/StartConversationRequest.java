package com.group5.lostandfoundjava.dto.chat;

import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "itemId is required")
    private UUID itemId;
}
