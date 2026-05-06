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
            // Unpack custom Result structure if returned by registration-service, 
            // but the controller returns ResponseUtility.toResponse(result) which could be Result<?> or standard wrapper
            // Assuming we get a standard structure containing data
            // To simplify, if the response is Result<List<RegistrationDto>>, we need a wrapper or just map it.
            // Let's use a generic map or custom deserialization if needed.
            // We'll assume the API returns the list directly or wrapped in data.
            // Wait, looking at RegistrationController: ResponseUtility.toResponse(result)
            // Usually returns {"data": [...], "success": true, ...}
            
            ResponseEntity<RegistrationResponseWrapper> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    RegistrationResponseWrapper.class
            );
            
            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData();
            }
            return List.of();

        } catch (Exception e) {
            log.error("[HTTP] Failed to call Registration Service: {}", e.getMessage());
            return List.of();
        }
    }

    private static class RegistrationResponseWrapper {
        private List<RegistrationDto> data;
        private boolean success;
        
        public List<RegistrationDto> getData() { return data; }
        public void setData(List<RegistrationDto> data) { this.data = data; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}
