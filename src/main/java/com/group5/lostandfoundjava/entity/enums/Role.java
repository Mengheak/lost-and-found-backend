package com.group5.lostandfoundjava.entity.enums;

public enum Role {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Role fromNameOrDefault(String name) {
        for (Role role : values()) {
            if (role.name().equals(name)) {
                return role;
            }
        }
        return USER;
    }
}
