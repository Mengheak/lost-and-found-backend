package com.group5.lostandfoundjava.entity.enums;

import static com.group5.lostandfoundjava.entity.enums.Permission.ADMIN_CREATE;
import static com.group5.lostandfoundjava.entity.enums.Permission.ADMIN_DELETE;
import static com.group5.lostandfoundjava.entity.enums.Permission.ADMIN_READ;
import static com.group5.lostandfoundjava.entity.enums.Permission.ADMIN_UPDATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * What an account is allowed to do. The role is stored on the user row and copied into every access
 * token, so authorising a request needs no database lookup.
 *
 * <p>Each role carries a set of {@link Permission}s. {@link #getAuthorities()} flattens the role and
 * its permissions into the list Spring Security actually checks: {@code ROLE_ADMIN} backs
 * {@code hasRole("ADMIN")}, while {@code admin:delete} backs {@code hasAuthority("admin:delete")}.
 */
@RequiredArgsConstructor
public enum Role {
    USER(Collections.emptySet()),
    ADMIN(Set.of(ADMIN_READ, ADMIN_UPDATE, ADMIN_CREATE, ADMIN_DELETE)),
    ;

    @Getter
    private final Set<Permission> permissions;

    /** The {@code ROLE_}-prefixed name on its own, without the permissions. */
    public String authority() {
        return "ROLE_" + name();
    }

    /** Every authority this role grants: one per permission, plus the role itself. */
    public List<SimpleGrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>(permissions.size() + 1);
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getPermission())));
        authorities.add(new SimpleGrantedAuthority(authority()));
        return authorities;
    }

    /**
     * Reads a role name that came from outside the application — a token claim, for example. An
     * unknown or missing name falls back to the least privileged role rather than failing, so a
     * tampered token can only ever lose access, never gain it.
     */
    public static Role fromNameOrDefault(String name) {
        for (Role role : values()) {
            if (role.name().equals(name)) {
                return role;
            }
        }
        return USER;
    }
}
