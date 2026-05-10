package com.example.paymentservice.Config;

import com.example.paymentservice.DTOs.ErrorResponse;
import com.example.paymentservice.Exceptions.DataConflictException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for all REST controllers.
 * Converts exceptions to clean error responses without exposing internal details.
 * Logs full details internally for debugging.
 * 
 * Rules:
 * - NEVER expose: class names, SQL errors, stack traces, Spring internals, field names
 * - ALWAYS give actionable, user-friendly messages
 * - Log full exception internally: log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle IllegalArgumentException → 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        String code = "BAD_REQUEST";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                "The data you provided is not valid. Please check your input.",
                code
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle validation errors from @Valid annotations → 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        BindingResult bindingResult = ex.getBindingResult();
        String errors = bindingResult.getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        String code = "VALIDATION_ERROR";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — Validation Error: {}", code, method, uri, errors, ex);
        
        ErrorResponse error = new ErrorResponse(
                "Validation failed: " + errors,
                code
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle EntityNotFoundException and NoSuchElementException → 404 Not Found
     */
    @ExceptionHandler({
            EntityNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {
        
        String code = "NOT_FOUND";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                "The requested item was not found.",
                code
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle DataConflictException (duplicate email, etc.) → 409 Conflict
     */
    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<ErrorResponse> handleDataConflictException(
            DataConflictException ex,
            HttpServletRequest request) {
        
        String code = "CONFLICT";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                code
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle DataIntegrityViolationException (duplicate keys, foreign key violations) → 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        String cause = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        String code = "CONFLICT";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — DataIntegrityViolation: {}", code, method, uri, cause, ex);
        
        ErrorResponse error = new ErrorResponse(
                "This record already exists. Please check for duplicates.",
                code
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle RuntimeException (catch-all for service-level exceptions) → 400 Bad Request
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        String code = "BAD_REQUEST";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                "Something went wrong. Please try again.",
                code
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other Exceptions → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        String code = "INTERNAL_SERVER_ERROR";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                "An unexpected error occurred. Please contact support.",
                code
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
