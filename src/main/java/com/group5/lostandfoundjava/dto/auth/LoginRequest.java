package com.group5.lostandfoundjava.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class LoginRequest {

    @NotBlank(message = "email is required")
    @Email(message = "invalid email address")
    private String email;

    @NotBlank(message = "password is required")
    private String password;
}
