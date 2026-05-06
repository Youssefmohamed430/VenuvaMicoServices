package com.example.notif_service.Client;

import com.example.notif_service.DTOs.RegistrationDto;
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
public class RegistrationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.registration.url}")
    private String registrationServiceUrl;

    public List<RegistrationDto> getRegistrationsByEventId(int eventId) {
        String url = registrationServiceUrl + "/api/registrations/event/" + eventId;
        log.info("[HTTP] Calling Registration Service: GET {}", url);

        try {
            ResponseEntity<List<RegistrationDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<RegistrationDto>>() {}
            );
            
            if (response.getBody() != null) {
                return response.getBody();
            }
            return List.of();

        } catch (Exception e) {
            log.error("[HTTP] Failed to call Registration Service: {}", e.getMessage());
            return List.of();
        }
    }
}
