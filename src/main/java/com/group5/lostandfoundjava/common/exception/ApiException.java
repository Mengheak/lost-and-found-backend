package com.group5.lostandfoundjava.common.exception;

/**
 * Base class for the exceptions this application throws on purpose.
 *
 * <p>Each subclass maps to one HTTP status code in
 * {@link com.group5.lostandfoundjava.common.GlobalExceptionHandler}, so a service can simply throw
 * {@code NotFoundException} and never has to know about {@code ResponseEntity} or status codes.
 *
 * <p>It extends {@link RuntimeException} (an unchecked exception) so callers are not forced to
 * declare or catch it — Spring's exception handler deals with it centrally.
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
