package com.example.event_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.event_service.domain.EventStatus;

public class EventDtos {

    @Data
    public static class CreateEventDto {
        @NotBlank
        private String title;
        private String description;
        @NotNull
        private LocalDateTime date;
        private String location;
        private int maxAttendance;
        private int organizerId;
        private int categoryId;
        private EventStatus eventStatus;
        private boolean paymentRequired;
        private BigDecimal price;
    }

    @Data
    public static class AllEventsDto {
        private int id;
        private String title;
        private LocalDateTime date;
        private String location;
        private EventStatus eventStatus;
        private boolean paymentRequired;
        private BigDecimal price;
        private String categoryName;
        // اسم الـ organizer بنجيبه من auth-service
        private String organizerName;
    }

    @Data
    public static class DetailedEventDto {
        private int id;
        private String title;
        private String description;
        private LocalDateTime date;
        private String location;
        private int maxAttendance;
        private EventStatus eventStatus;
        private boolean paymentRequired;
        private BigDecimal price;
        private int organizerId;
        private String organizerName;
        private int categoryId;
        private String categoryName;
    }

    @Data
    public static class CategoryDto {
        private int id;
        private String name;
    }

    // Event created event — بنبعتها على RabbitMQ
    @Data
    public static class EventCreatedEvent {
        private int eventId;
        private String title;
        private String date;
        private String location;
        private String message;
    }

    // Event updated event — بنبعتها على RabbitMQ
    @Data
    public static class EventUpdatedEvent {
        private int eventId;
        private String title;
        private String date;
        private String location;
        private String message;
    }
}
