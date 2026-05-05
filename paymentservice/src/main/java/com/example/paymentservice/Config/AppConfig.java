package com.example.paymentservice.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {


    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Register the request logging filter to log all HTTP requests and responses.
     */
    @Bean
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }
    
}
