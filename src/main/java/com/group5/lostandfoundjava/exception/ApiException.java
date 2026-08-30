package com.group5.lostandfoundjava.exception;


public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
