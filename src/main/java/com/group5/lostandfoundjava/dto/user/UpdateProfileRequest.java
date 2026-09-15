package com.group5.lostandfoundjava.dto.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// A partial update: every null field means "not sent", so it keeps its current value
@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class UpdateProfileRequest {

    @Size(min = 1, max = 255, message = "name length must be between 1 and 255")
    private String name;

    @Size(max = 50, message = "phone length must be less than 50")
    private String phone;

    @Size(max = 1024, message = "profilePhotoUrl length must be less than 1024")
    private String profilePhotoUrl;
}
