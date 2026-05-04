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
        } catch (IllegalArgumentException e) {
            logger.warn("خطأ في المدخلات: {}", e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "المدخلات غير صحيحة", e);
        } catch (NullPointerException e) {
            logger.error("خطأ Null Pointer: {}", e.getMessage());
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "خطأ داخلي في النظام", e);
        } catch (Exception e) {
            logger.error("خطأ غير متوقع: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "حدث خطأ غير متوقع", e);
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