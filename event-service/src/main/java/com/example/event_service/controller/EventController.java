package com.example.event_service.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.event_service.dto.EventDtos.*;
import com.example.event_service.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<AllEventsDto>> getAll() {
        return ResponseEntity.ok(eventService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailedEventDto> getById(@PathVariable int id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Integer> create(@Valid @RequestBody CreateEventDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.add(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> update(@PathVariable int id,
            @Valid @RequestBody DetailedEventDto dto) {
        return ResponseEntity.ok(eventService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> delete(@PathVariable int id) {
        return ResponseEntity.ok(eventService.delete(id));
    }

    // Internal endpoint — Registration Service بتكلمه
    @GetMapping("/internal/{id}")
    public ResponseEntity<DetailedEventDto> getEventInternal(@PathVariable int id) {
        return ResponseEntity.ok(eventService.getEventForRegistration(id));
    }
}
