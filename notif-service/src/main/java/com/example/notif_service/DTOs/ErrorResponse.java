package com.example.notif_service.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for all API errors.
 * Never exposes stack traces, internal class names, or SQL errors to clients.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success = false;
    private String message;
    private String code;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, String code) {
        this.success = false;
        this.message = message;
        this.code = code;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message) {
        this.success = false;
        this.message = message;
        this.code = "ERROR";
        this.timestamp = LocalDateTime.now();
    }
}
