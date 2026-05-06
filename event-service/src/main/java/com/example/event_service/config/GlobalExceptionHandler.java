package com.example.event_service.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        String code = "FORBIDDEN";
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.error("[{}] {} {} — {}", code, method, uri, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
                "You do not have permission to perform this action.",
                code
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

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
