package com.group5.lostandfoundjava.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// A single fine-grained capability, granted to a caller through their Role
@RequiredArgsConstructor
public enum Permission {
    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    ;

    @Getter
    private final String permission;
}
