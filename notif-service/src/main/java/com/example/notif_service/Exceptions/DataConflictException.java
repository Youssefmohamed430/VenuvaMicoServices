package com.example.notif_service.Exceptions;

/**
 * Exception thrown when a data conflict occurs (e.g., duplicate email, duplicate record).
 * Should result in HTTP 409 Conflict response.
 */
public class DataConflictException extends RuntimeException {
    public DataConflictException(String message) {
        super(message);
    }

    public DataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
