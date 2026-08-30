package com.group5.lostandfoundjava.entity.enums;

/** What a user is allowed to do. Stored in the {@code users.role} column as text. */
public enum Role {
    USER,
    ADMIN;

    /**
     * The name Spring Security expects. {@code hasRole("ADMIN")} looks for the authority
     * {@code "ROLE_ADMIN"}, so the prefix is added here in one place.
     */
    public String authority() {
        return "ROLE_" + name();
    }

    /** Parses a role name, falling back to {@link #USER} for anything unknown or missing. */
    public static Role fromNameOrDefault(String name) {
        for (Role role : values()) {
            if (role.name().equals(name)) {
                return role;
            }
        }
        return USER;
    }
}
