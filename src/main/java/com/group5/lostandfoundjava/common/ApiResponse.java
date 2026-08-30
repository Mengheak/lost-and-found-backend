package com.group5.lostandfoundjava.common;

/**
 * The single envelope every endpoint returns, so clients always parse the same shape:
 *
 * <pre>{ "success": true, "message": "Success", "data": { ... } }</pre>
 *
 * @param <T> type of the payload carried in {@code data}
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    /** Successful response with a payload and the default message. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    /** Successful response with a payload and a custom message. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /** Successful response that carries no payload, e.g. after a delete. */
    public static <T> ApiResponse<T> message(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /** Failed response. Used by {@link GlobalExceptionHandler}. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
