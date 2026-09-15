package com.group5.lostandfoundjava.dto.user;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The few user fields shown next to something else — an item, a message, a rating. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private UUID id;

    private String name;

    private String profilePhotoUrl;

    private double ratingAvg;
}
