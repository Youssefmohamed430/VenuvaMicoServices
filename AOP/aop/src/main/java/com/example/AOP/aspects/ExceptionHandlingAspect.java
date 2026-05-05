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

            if (e instanceof IllegalArgumentException) {
                logger.warn("خطأ في المدخلات: {}", e.getMessage());
                return createErrorResponse(HttpStatus.BAD_REQUEST, "المدخلات غير صحيحة", (Exception) e);
            } else if (e instanceof NullPointerException) {
                logger.error("خطأ Null Pointer: {}", e.getMessage());
                return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "خطأ داخلي في النظام", (Exception) e);
            } else {
                logger.error("خطأ غير متوقع: {}", e.getMessage(), e);
                return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "حدث خطأ غير متوقع", (Exception) e);
            }
        }
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String message, Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.value());
        errorResponse.put("message", message);
        errorResponse.put("error", exception.getClass().getSimpleName());
        errorResponse.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(errorResponse, status);
    }
}