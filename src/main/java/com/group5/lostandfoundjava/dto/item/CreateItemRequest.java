package com.group5.lostandfoundjava.dto.item;

import com.group5.lostandfoundjava.entity.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
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

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class CreateItemRequest {

    private ItemType type;

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name length must be less than 255")
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

    /** Only meaningful on a LOST item; the service rejects it on a FOUND one. */
    @PositiveOrZero(message = "rewardAmount must not be negative")
    private BigDecimal rewardAmount;

    /** Only meaningful on a FOUND item; the service rejects it on a LOST one. */
    @Size(max = 255, message = "storageLocation length must be less than 255")
    private String storageLocation;
}
