package com.group5.lostandfoundjava.dto.item;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// A partial update: every null field is skipped, so a client sends only what changed
@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class UpdateItemRequest {

    @Size(min = 1, max = 255, message = "name length must be between 1 and 255")
    private String name;

    private UUID categoryId;

    @Size(max = 10_000, message = "description length must be less than 10000")
    private String description;

    @Size(max = 100, message = "brand length must be less than 100")
    private String brand;

    @Size(max = 50, message = "color length must be less than 50")
    private String color;

    @Size(max = 10, message = "photoUrls must contain at most 10 items")
    private List<String> photoUrls;

    private Double locationLat;

    private Double locationLng;

    private Instant dateTime;

    @PositiveOrZero(message = "rewardAmount must not be negative")
    private BigDecimal rewardAmount;

    @Size(max = 255, message = "storageLocation length must be less than 255")
    private String storageLocation;
}
