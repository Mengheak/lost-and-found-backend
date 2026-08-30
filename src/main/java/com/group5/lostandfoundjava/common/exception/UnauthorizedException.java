package com.group5.lostandfoundjava.common.exception;

/** Translated into HTTP 401 — the caller is not signed in, or the token is invalid. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
