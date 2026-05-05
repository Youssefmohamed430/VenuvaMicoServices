package com.example.registration_service.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.registration_service.DTOs.EventDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Synchronous HTTP client for Event Service.
 * Used to fetch event data when registration-service needs to validate event info.
 * Endpoint: GET /api/events/internal/{id}
 */
@Component
@Slf4j
public class EventServiceClient {

    private final RestTemplate restTemplate;
    private final String eventServiceUrl;

    public EventServiceClient(RestTemplate restTemplate,
            @Value("${services.event.url:http://localhost:8088}") String eventServiceUrl) {
        this.restTemplate = restTemplate;
        this.eventServiceUrl = eventServiceUrl;
    }

    /**
     * Fetches event by ID from Event Service (Sync).
     * Returns null if event not found or service is unavailable.
     */
    public EventDto getEventById(int eventId) {
        try {
            log.info("[HTTP] EventServiceClient.getEventById() -> GET {}/api/events/internal/{}", eventServiceUrl, eventId);
            EventDto event = restTemplate.getForObject(
                eventServiceUrl + "/api/events/internal/" + eventId,
                EventDto.class
            );
            log.info("[HTTP] EventServiceClient.getEventById() <- Found event id={}", eventId);
            return event;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[HTTP] EventServiceClient.getEventById() <- 404 Event not found: {}", eventId);
            return null;
        } catch (Exception e) {
            log.error("[HTTP] EventServiceClient.getEventById() <- ERROR: {}", e.getMessage());
            return null;
        }
    }
}
