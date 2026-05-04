package com.example.authservice.Config;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP request/response logging filter.
 * Logs HTTP method, URI, and response status code for every request.
 * Runs once per request to provide unified logging across the application.
 * 
 * Excludes: /error, /favicon.ico
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        
        // Skip logging for excluded paths
        if (uri.equals("/error") || uri.equals("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        
        try {
            // Process the request
            filterChain.doFilter(request, response);
        } finally {
            // Log after response is generated
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            log.info("[REQUEST] {} {} → {} ({}ms)", method, uri, status, duration);
        }
    }
}
