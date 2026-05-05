package com.example.event_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.event_service.domain.Event;
import com.example.event_service.domain.EventStatus;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByOrganizerId(int organizerId);
    List<Event> findByEventStatus(EventStatus status);
}
