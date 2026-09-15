package com.group5.lostandfoundjava.entity.enums;

/**
 * The HTTP authentication scheme a stored {@link com.group5.lostandfoundjava.entity.Token} is used
 * with. Only bearer tokens are issued today; the enum exists so adding another scheme later does not
 * require a schema change.
 */
public enum TokenType {
    BEARER
}
