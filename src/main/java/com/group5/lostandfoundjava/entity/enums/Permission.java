package com.group5.lostandfoundjava.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A single fine-grained capability, granted to a caller through their {@link Role}.
 *
 * <p>Roles answer "who is this?", permissions answer "what may they do?". Keeping the two apart
 * means an endpoint can require {@code admin:delete} without caring which role happens to carry it,
 * so re-shuffling permissions between roles later touches no endpoint.
 */
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
