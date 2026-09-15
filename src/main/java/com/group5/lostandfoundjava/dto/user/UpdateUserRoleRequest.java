package com.group5.lostandfoundjava.dto.user;

import com.group5.lostandfoundjava.entity.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor // required by Jackson to deserialise @RequestBody
@AllArgsConstructor // @Builder needs it back once an explicit constructor is declared
public class UpdateUserRoleRequest {

    @NotNull(message = "role is required")
    private Role role;
}
