package com.example.registration_service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate bean used by inter-service HTTP clients:
     * - AuthServiceClient: calls Auth Service
     * - EventServiceClient: calls Event Service
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
