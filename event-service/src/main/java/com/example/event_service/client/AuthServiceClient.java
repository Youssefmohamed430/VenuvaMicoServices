package com.example.event_service.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP Client للتواصل مع Auth Service
 * بنستخدمه لما نحتاج اسم الـ organizer
 */
@Component
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthServiceClient(RestTemplate restTemplate,
                            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl; // FIX: was hardcoded to "authServiceUrl" string literal
    }

    public OrganizerDto getOrganizerById(int organizerId) {
        try {
            // FIX: was /api/admin/organizers/{id} — now uses the correct internal endpoint
            return restTemplate.getForObject(
                authServiceUrl + "/api/auth/organizers/" + organizerId,
                OrganizerDto.class
            );
        } catch (Exception e) {
            OrganizerDto fallback = new OrganizerDto();
            fallback.setId(organizerId);
            fallback.setName("Unknown Organizer");
            return fallback;
        }
    }

    @Data
    public static class OrganizerDto {
        private int id;
        private String name;
        private String email;
    }
}
