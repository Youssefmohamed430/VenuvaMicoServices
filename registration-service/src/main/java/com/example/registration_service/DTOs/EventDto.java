package com.example.registration_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for event data fetched from Event Service via REST API.
 * Maps to the response of GET /api/events/internal/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private int id;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private int maxAttendance;
    private String eventStatus;
    private boolean paymentRequired;
    private BigDecimal price;
    private int organizerId;
    private String organizerName;
    private int categoryId;
    private String categoryName;
}
