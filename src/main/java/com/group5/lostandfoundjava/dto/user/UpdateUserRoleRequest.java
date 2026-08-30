package com.group5.lostandfoundjava.dto.user;

import com.group5.lostandfoundjava.entity.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull Role role) {}
