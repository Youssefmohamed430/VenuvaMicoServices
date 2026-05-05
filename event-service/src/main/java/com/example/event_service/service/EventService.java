package com.example.event_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.event_service.client.AuthServiceClient;
import com.example.event_service.domain.Category;
import com.example.event_service.domain.Event;
import com.example.event_service.dto.EventDtos.AllEventsDto;
import com.example.event_service.dto.EventDtos.CreateEventDto;
import com.example.event_service.dto.EventDtos.DetailedEventDto;
import com.example.event_service.dto.EventDtos.EventCreatedEvent;
import com.example.event_service.messaging.EventPublisher;
import com.example.event_service.repository.CategoryRepository;
import com.example.event_service.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final AuthServiceClient authServiceClient; // HTTP call to auth-service
    private final EventPublisher eventPublisher;       // RabbitMQ publisher

    public int add(CreateEventDto dto) {
        log.info("[START] EventService.add() — title='{}', organizerId={}", dto.getTitle(), dto.getOrganizerId());

        if (dto.getDate().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Event date must be in the future");
        if (dto.getMaxAttendance() <= 0)
            throw new RuntimeException("MaxAttendance must be greater than 0");
        if (dto.isPaymentRequired() && dto.getPrice() == null)
            throw new RuntimeException("Price must be provided when payment is required");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Event event = Event.builder()
                .title(dto.getTitle()).description(dto.getDescription())
                .date(dto.getDate()).location(dto.getLocation())
                .category(category).organizerId(dto.getOrganizerId())
                .maxAttendance(dto.getMaxAttendance()).eventStatus(dto.getEventStatus())
                .paymentRequired(dto.isPaymentRequired()).price(dto.getPrice())
                .build();

        eventRepository.save(event);

        // Publish event.created → Notification Service سيسمعها ويبعت notifications
        String formattedDate = event.getDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        EventCreatedEvent createdEvent = new EventCreatedEvent();
        createdEvent.setEventId(event.getId());
        createdEvent.setTitle(event.getTitle());
        createdEvent.setDate(formattedDate);
        createdEvent.setLocation(event.getLocation());
        createdEvent.setMessage("New event: " + event.getTitle() + " on " + formattedDate + " at " + event.getLocation());
        eventPublisher.publishEventCreated(createdEvent);

        log.info("[OK] EventService.add() — Event {} created", event.getId());
        return event.getId();
    }

    public List<AllEventsDto> getAll() {
        return eventRepository.findAll().stream().map(event -> {
            AllEventsDto dto = new AllEventsDto();
            dto.setId(event.getId());
            dto.setTitle(event.getTitle());
            dto.setDate(event.getDate());
            dto.setLocation(event.getLocation());
            dto.setEventStatus(event.getEventStatus());
            dto.setPaymentRequired(event.isPaymentRequired());
            dto.setPrice(event.getPrice());
            if (event.getCategory() != null)
                dto.setCategoryName(event.getCategory().getName());
            // HTTP call إلى Auth Service لجلب اسم الـ organizer
            try {
                var organizer = authServiceClient.getOrganizerById(event.getOrganizerId());
                dto.setOrganizerName(organizer != null ? organizer.getName() : "Unknown");
            } catch (Exception e) {
                dto.setOrganizerName("Unknown");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public DetailedEventDto getById(int id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        DetailedEventDto dto = new DetailedEventDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate());
        dto.setLocation(event.getLocation());
        dto.setMaxAttendance(event.getMaxAttendance());
        dto.setEventStatus(event.getEventStatus());
        dto.setPaymentRequired(event.isPaymentRequired());
        dto.setPrice(event.getPrice());
        dto.setOrganizerId(event.getOrganizerId());
        if (event.getCategory() != null) {
            dto.setCategoryId(event.getCategory().getId());
            dto.setCategoryName(event.getCategory().getName());
        }
        try {
            var organizer = authServiceClient.getOrganizerById(event.getOrganizerId());
            dto.setOrganizerName(organizer != null ? organizer.getName() : "Unknown");
        } catch (Exception e) { dto.setOrganizerName("Unknown"); }
        return dto;
    }

    public boolean update(int id, DetailedEventDto dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getDate() != null) event.setDate(dto.getDate());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getEventStatus() != null) event.setEventStatus(dto.getEventStatus());
        event.setMaxAttendance(dto.getMaxAttendance());
        eventRepository.save(event);
        return true;
    }

    public boolean delete(int id) {
        eventRepository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));
        eventRepository.deleteById(id);
        return true;
    }

    // Used by Registration Service via HTTP
    public DetailedEventDto getEventForRegistration(int eventId) {
        return getById(eventId);
    }
}
