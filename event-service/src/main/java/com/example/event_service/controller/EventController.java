package com.example.event_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.AOP.Annotation.HandleException;
import com.example.AOP.Annotation.Loggable;
import com.example.event_service.dto.EventDtos.*;
import com.example.event_service.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    @GetMapping
    @HandleException
    @Loggable(value = "GetAllEvents", logArguments = false, logResult = false)
    public ResponseEntity<List<AllEventsDto>> getAll() {
        log.info("[PUBLIC] EventController.getAll()");
        return ResponseEntity.ok(eventService.getAll());
    }

    @GetMapping("/{id}")
    @HandleException
    @Loggable(value = "GetEventById", logArguments = true, logResult = false)
    public ResponseEntity<DetailedEventDto> getById(@PathVariable int id) {
        log.info("[PUBLIC] EventController.getById() — id={}", id);
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "CreateEvent", logArguments = false, logResult = false)
    public ResponseEntity<Integer> create(@Valid @RequestBody CreateEventDto dto) {
        log.info("[ORGANIZER/ADMIN] EventController.create()");
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.add(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "UpdateEvent", logArguments = true, logResult = false)
    public ResponseEntity<Boolean> update(@PathVariable int id,
            @Valid @RequestBody DetailedEventDto dto) {
        log.info("[ORGANIZER/ADMIN] EventController.update() — id={}", id);
        return ResponseEntity.ok(eventService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "DeleteEvent", logArguments = true, logResult = false)
    public ResponseEntity<Boolean> delete(@PathVariable int id) {
        log.info("[ADMIN] EventController.delete() — id={}", id);
        return ResponseEntity.ok(eventService.delete(id));
    }

    /**
     * GET /api/events/internal/{id}
     * Internal endpoint — called by Registration Service (Sync REST)
     * to fetch event details before creating a registration.
     */
    @GetMapping("/internal/{id}")
    @HandleException
    public ResponseEntity<DetailedEventDto> getEventInternal(@PathVariable int id) {
        log.info("[INTERNAL] EventController.getEventInternal() — id={}", id);
        return ResponseEntity.ok(eventService.getEventForRegistration(id));
    }
}
