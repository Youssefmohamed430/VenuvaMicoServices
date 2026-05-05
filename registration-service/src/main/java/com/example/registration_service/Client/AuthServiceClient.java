package com.example.registration_service.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.registration_service.DTOs.UserDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Synchronous HTTP client for Auth Service.
 * Used to fetch user data when registration-service needs to validate or enrich user info.
 * Endpoint: GET /api/auth/users/{id}
 */
@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthServiceClient(RestTemplate restTemplate,
            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    /**
     * Fetches user by ID from Auth Service (Sync).
     * Returns null if user not found or service is unavailable.
     */
    public UserDto getUserById(int userId) {
        try {
            log.info("[HTTP] AuthServiceClient.getUserById() -> GET {}/api/auth/users/{}", authServiceUrl, userId);
            UserDto user = restTemplate.getForObject(
                authServiceUrl + "/api/auth/users/" + userId,
                UserDto.class
            );
            log.info("[HTTP] AuthServiceClient.getUserById() <- Found user id={}", userId);
            return user;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[HTTP] AuthServiceClient.getUserById() <- 404 User not found: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("[HTTP] AuthServiceClient.getUserById() <- ERROR: {}", e.getMessage());
            return null;
        }
    }
}
