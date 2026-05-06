package com.example.notif_service.Client;

import com.example.notif_service.DTOs.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.auth.url}")
    private String authServiceUrl;

    public List<UserDto> getAllAttendees() {
        String url = authServiceUrl + "/api/auth/users/role/ATTENDEE";
        log.info("[HTTP] Calling Auth Service: GET {}", url);

        try {
            ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("[HTTP] Failed to call Auth Service: {}", e.getMessage());
            return List.of();
        }
    }
}
