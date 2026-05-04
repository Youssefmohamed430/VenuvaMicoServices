package com.example.authservice.Config;

import com.example.authservice.Abstractions.*; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for converting Result<T> objects to proper ResponseEntity with correct HTTP status codes.
 * Examines error messages to determine the appropriate HTTP status code.
 * 
 * CRITICAL: Never return HTTP 200 with a failed Result.
 * CRITICAL: All error bodies must use ErrorResponse DTO.
 */
public class ResponseUtility {

    /**
     * Convert a Result<T> to ResponseEntity with proper HTTP status codes.
     * 
     * Rules:
     * - If success: return 200 OK with the value
     * - If failure with "not found": return 404 NOT_FOUND
     * - If failure with "already" or "duplicate": return 409 CONFLICT
     * - If failure with "unauthorized": return 401 UNAUTHORIZED
     * - If failure with "forbidden": return 403 FORBIDDEN
     * - If failure with "invalid" or "missing": return 400 BAD_REQUEST
     * - Default failure: return 400 BAD_REQUEST
     * 
     * @param result The Result<T> from service layer
     * @return ResponseEntity with appropriate status and error response
     */
    public static <T> ResponseEntity<?> toResponse(Result<T> result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        }

        String message = result.getError().getMessage();
        String lowerMessage = message.toLowerCase();

        // 404 Not Found
        if (lowerMessage.contains("not found")) {
            return status(404, result, "NOT_FOUND");
        }

        // 409 Conflict (duplicate, already registered, etc.)
        if (lowerMessage.contains("already") || lowerMessage.contains("duplicate")) {
            return status(409, result, "CONFLICT");
        }

        // 401 Unauthorized
        if (lowerMessage.contains("unauthorized")) {
            return status(401, result, "UNAUTHORIZED");
        }

        // 403 Forbidden
        if (lowerMessage.contains("forbidden")) {
            return status(403, result, "FORBIDDEN");
        }

        // 400 Bad Request (invalid, missing, invalid request body, etc.)
        if (lowerMessage.contains("invalid") || lowerMessage.contains("missing") 
                || lowerMessage.contains("request")) {
            return status(400, result, "BAD_REQUEST");
        }

        // Default: 400 Bad Request
        return status(400, result, "BAD_REQUEST");
    }

    /**
     * Helper method to construct error response with proper status code.
     */
    private static <T> ResponseEntity<?> status(int code, Result<T> result, String errorCode) {
        return ResponseEntity.status(code).body(
            new ErrorResponse(result.getError().getMessage(), errorCode)
        );
    }

    /**
     * Convert a Result<T> to ResponseEntity with a specific success status code.
     * Useful for POST operations that should return 201 CREATED instead of 200 OK.
     * 
     * CRITICAL: Failure results still return error status codes (never 200 for failures).
     * 
     * @param result The Result<T> from service layer
     * @param successStatus The HTTP status to use on success (e.g., HttpStatus.CREATED)
     * @return ResponseEntity with appropriate status and response body
     */
    public static <T> ResponseEntity<?> toResponse(Result<T> result, HttpStatus successStatus) {
        if (result.isSuccess()) {
            return ResponseEntity.status(successStatus).body(result.getValue());
        }
        return toResponse(result);
    }
}
