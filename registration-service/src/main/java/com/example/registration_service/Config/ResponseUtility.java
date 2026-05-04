package com.example.registration_service.Config;

import com.example.registration_service.Abstractions.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseUtility {

    private ResponseUtility() {
    }

    public static <T> ResponseEntity<?> toResponse(Result<T> result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }

        String message = result.getError().getMessage();
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("not found")) {
            return status(404, result, "NOT_FOUND");
        }

        if (lowerMessage.contains("already") || lowerMessage.contains("duplicate")) {
            return status(409, result, "CONFLICT");
        }

        if (lowerMessage.contains("unauthorized")) {
            return status(401, result, "UNAUTHORIZED");
        }

        if (lowerMessage.contains("forbidden")) {
            return status(403, result, "FORBIDDEN");
        }

        if (lowerMessage.contains("invalid") || lowerMessage.contains("missing")
                || lowerMessage.contains("request")) {
            return status(400, result, "BAD_REQUEST");
        }

        return status(400, result, "BAD_REQUEST");
    }

    private static <T> ResponseEntity<?> status(int code, Result<T> result, String errorCode) {
        return ResponseEntity.status(code).body(
                new ErrorResponse(result.getError().getMessage(), errorCode)
        );
    }

    public static <T> ResponseEntity<?> toResponse(Result<T> result, HttpStatus successStatus) {
        if (result.isSuccess()) {
            return ResponseEntity.status(successStatus).body(result.getValue());
        }
        return toResponse(result);
    }
}
