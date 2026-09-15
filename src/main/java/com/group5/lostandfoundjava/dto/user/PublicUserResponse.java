package com.group5.lostandfoundjava.dto.user;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Somebody else's profile
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserResponse {

    private UUID id;

    private String name;

    private String profilePhotoUrl;

    private double ratingAvg;

    private Instant memberSince;
}
