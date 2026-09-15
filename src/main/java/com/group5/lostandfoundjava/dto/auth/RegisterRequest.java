package com.group5.lostandfoundjava.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class RegisterRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name length must be less than 255")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "invalid email address")
    @Size(max = 255, message = "email length must be less than 255")
    private String email;

    @Size(max = 50, message = "phone length must be less than 50")
    private String phone;

    // bcrypt silently ignores anything past 72 bytes, so refusing it is clearer than truncating.
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
    private String password;
}
