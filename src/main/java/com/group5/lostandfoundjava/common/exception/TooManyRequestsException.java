package com.group5.lostandfoundjava.common.exception;

/** Translated into HTTP 429 — the caller is being rate limited. */
public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
