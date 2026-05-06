package com.example.AOP.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
@Aspect
public class ExceptionHandlingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingAspect.class);

 
    @Around("@annotation(com.example.AOP.Annotation.HandleException)")
    public Object handleException(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            // Ensure the method actually returns ResponseEntity before returning our JSON error
            if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
                org.aspectj.lang.reflect.MethodSignature methodSignature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
                if (!ResponseEntity.class.isAssignableFrom(methodSignature.getReturnType())) {
                    throw e; // Rethrow if it doesn't return ResponseEntity
                }
            }

            String exceptionName = e.getClass().getSimpleName();
            
            if (e instanceof IllegalArgumentException) {
                logger.warn("خطأ في المدخلات: {}", e.getMessage());
                return createErrorResponse(HttpStatus.BAD_REQUEST, "The data you provided is not valid. Please check your input.", "BAD_REQUEST");
            } else if (exceptionName.equals("InvalidCredentialsException")) {
                logger.warn("Authentication failed: {}", e.getMessage());
                return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), "UNAUTHORIZED");
            } else if (exceptionName.equals("ResourceNotFoundException") || exceptionName.equals("EntityNotFoundException") || exceptionName.equals("NoSuchElementException")) {
                logger.warn("Resource not found: {}", e.getMessage());
                return createErrorResponse(HttpStatus.NOT_FOUND, "The requested item was not found.", "NOT_FOUND");
            } else if (exceptionName.equals("DataConflictException") || exceptionName.equals("DataIntegrityViolationException")) {
                logger.warn("Data conflict: {}", e.getMessage());
                return createErrorResponse(HttpStatus.CONFLICT, e.getMessage() != null ? e.getMessage() : "This record already exists. Please check for duplicates.", "CONFLICT");
            } else if (exceptionName.equals("AccessDeniedException")) {
                logger.warn("Access denied: {}", e.getMessage());
                return createErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", "FORBIDDEN");
            } else if (e instanceof NullPointerException) {
                logger.error("خطأ Null Pointer: {}", e.getMessage(), e);
                return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact support.", "INTERNAL_SERVER_ERROR");
            } else {
                logger.error("خطأ غير متوقع: {}", e.getMessage(), e);
                // Return generic 400 for RuntimeExceptions to match GlobalExceptionHandler behavior
                if (e instanceof RuntimeException) {
                    return createErrorResponse(HttpStatus.BAD_REQUEST, "Something went wrong. Please try again.", "BAD_REQUEST");
                }
                return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact support.", "INTERNAL_SERVER_ERROR");
            }
        }
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String message, String code) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        errorResponse.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(errorResponse, status);
    }
}