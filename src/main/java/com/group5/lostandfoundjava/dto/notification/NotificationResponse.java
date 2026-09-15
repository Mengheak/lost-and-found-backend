package com.group5.lostandfoundjava.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.group5.lostandfoundjava.entity.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One entry of the notification feed
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;

    private NotificationType type;

    private String message;

    @JsonProperty("isRead")
    private boolean read;

    private Instant createdAt;
}
