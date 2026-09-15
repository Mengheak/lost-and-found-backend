package com.group5.lostandfoundjava.dto.user;

import com.group5.lostandfoundjava.entity.enums.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// The account as its own owner sees
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;

    private String name;

    private String email;

    private String phone;

    private String profilePhotoUrl;

    private double ratingAvg;

    private Role role;

    private Instant createdAt;
}
