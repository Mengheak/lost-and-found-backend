package com.group5.lostandfoundjava.common.exception;

/** Translated into HTTP 403 — the caller is signed in but is not allowed to touch this resource. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(message);
    }
}
