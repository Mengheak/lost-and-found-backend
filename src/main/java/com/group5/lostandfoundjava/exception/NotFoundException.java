package com.group5.lostandfoundjava.exception;

/** Translated into HTTP 404 — no such row. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message);
    }
}
