package com.group5.lostandfoundjava.common.exception;

/** Translated into HTTP 400 — the request itself is wrong — bad values, impossible combinations. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message);
    }
}
