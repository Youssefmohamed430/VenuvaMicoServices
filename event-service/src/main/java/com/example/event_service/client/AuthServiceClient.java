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

    public AuthServiceClient(RestTemplate restTemplate,/* ده لينك وهمي لغايه ما يتم الربط */
                            @Value("${services.auth.url:http://localhost:9999}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = "authServiceUrl";
    }

    public OrganizerDto getOrganizerById(int organizerId) {
        try {
            return restTemplate.getForObject(
                authServiceUrl + "/api/admin/organizers/" + organizerId,
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
