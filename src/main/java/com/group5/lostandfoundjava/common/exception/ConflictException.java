package com.group5.lostandfoundjava.common.exception;

/** Translated into HTTP 409 — the request clashes with data that already exists. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }
}
