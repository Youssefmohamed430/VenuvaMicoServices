package com.example.registration_service.Services;

import com.example.registration_service.Abstractions.Error;
import com.example.registration_service.Abstractions.Result;
import com.example.registration_service.Models.Event;
import com.example.registration_service.Models.Registration;
import com.example.registration_service.Models.UserEntity;
import com.example.registration_service.RegisterationDto.CancleRegisrationDto;
import com.example.registration_service.RegisterationDto.RegistrationDto;
import com.example.registration_service.RegisterationDto.RegistrationRequestDto;
import com.example.registration_service.RegistrationStatus;
import com.example.registration_service.Repos.EventRepository;
import com.example.registration_service.Repos.RegistrationRepository;
import com.example.registration_service.Repos.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService implements IRegistrationService {

    private final RegistrationRepository repository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public boolean isUserAlreadyRegistered(int userId, int eventId) {
        return repository.existsByUserIdAndEventId(userId, eventId);
    }

    @Override
    public Result<RegistrationDto> registerUserToEvent(RegistrationRequestDto requestDto) {
        log.info("[START] RegistrationService.registerUserToEvent() — userId={}, eventId={}",
                requestDto.getUserId(), requestDto.getEventId());

        if (isUserAlreadyRegistered(requestDto.getUserId(), requestDto.getEventId())) {
            log.warn("[WARN] RegistrationService.registerUserToEvent() — User {} already registered for event {}",
                    requestDto.getUserId(), requestDto.getEventId());
            return Result.failure(
                    new Error("User already registered for this event")
            );
        }
        Event event = eventRepository.findById(requestDto.getEventId())
                .orElse(null);

        if (event == null) {
            log.warn("[WARN] RegistrationService.registerUserToEvent() — Event not found: {}",
                    requestDto.getEventId());
            return Result.failure(
                    new Error("Event not found")
            );
        }
        UserEntity user = userRepository.findById(requestDto.getUserId())
                .orElse(null);

        if (user == null) {
            log.warn("[WARN] RegistrationService.registerUserToEvent() — User not found: {}",
                    requestDto.getUserId());
            return Result.failure(
                    new Error("User not found")
            );
        }

        Registration registration = new Registration();

        registration.setUserId(requestDto.getUserId());
        registration.setEventId(requestDto.getEventId());

        registration.setRegistrationStatus(event.isPaymentRequired()
                ? RegistrationStatus.PENDING
                : RegistrationStatus.PAID);

        repository.save(registration);

        RegistrationDto dto = new RegistrationDto();
        dto.setRegistrationId(registration.getId());
        dto.setUserId(user.getId());
        dto.setEventId(event.getId());
        dto.setEventTitle(event.getTitle());
        dto.setEventDate(event.getDate());
        dto.setEventLocation(event.getLocation());
        dto.setPaymentRequired(event.isPaymentRequired());
        dto.setStatus(registration.getRegistrationStatus().toString());

        log.info("[OK] RegistrationService.registerUserToEvent() — User {} registered to event {}",
                requestDto.getUserId(), requestDto.getEventId());
        return Result.success(dto);
    }

    @Override
    public Result<Boolean> cancelRegistration(CancleRegisrationDto dto) {
        log.info("[START] RegistrationService.cancelRegistration() — userId={}, eventId={}",
                dto.getUserId(), dto.getEventId());

        if (dto == null) {
            log.warn("[WARN] RegistrationService.cancelRegistration() — Request body is missing");
            return Result.failure(new Error("Request body is missing"));
        }
        Registration registration = repository.findByEventIdAndUserId(dto.getEventId(), dto.getUserId());

        if (registration == null) {
            log.warn("[WARN] RegistrationService.cancelRegistration() — User {} not registered for event {}",
                    dto.getUserId(), dto.getEventId());
            return Result.failure(
                    new Error("User is not registered for this event")
            );
        }
        repository.delete(registration);
        log.info("[OK] RegistrationService.cancelRegistration() — User {} unregistered from event {}",
                dto.getUserId(), dto.getEventId());
        return Result.success(true);
    }

    @Override
    public Result<List<RegistrationDto>> getUserRegistrations(int userId) {
        log.info("[START] RegistrationService.getUserRegistrations() — userId={}", userId);

        List<Registration> registrations = repository.findByUserId(userId);

        if (registrations == null || registrations.isEmpty()) {
            log.warn("[WARN] RegistrationService.getUserRegistrations() — No registrations found for userId={}", userId);
            return Result.failure(
                    new Error("No registrations found")
            );
        }

        List<RegistrationDto> result = registrations.stream().map(r -> {

            Event event = eventRepository.findById(r.getEventId()).orElse(null);

            RegistrationDto dto = new RegistrationDto();
            dto.setRegistrationId(r.getId());
            dto.setUserId(r.getUserId());
            dto.setEventId(r.getEventId());

            if (event != null) {
                dto.setEventTitle(event.getTitle());
                dto.setEventDate(event.getDate());
                dto.setEventLocation(event.getLocation());
                dto.setPaymentRequired(event.isPaymentRequired());
            }

            dto.setStatus(r.getRegistrationStatus().toString());

            return dto;

        }).collect(Collectors.toList());

        log.info("[OK] RegistrationService.getUserRegistrations() — {} registrations retrieved for userId={}",
                result.size(), userId);
        return Result.success(result);
    }

    @Override
    public Result<Integer> getNumberOfRegesters() {
        log.info("[START] RegistrationService.getNumberOfRegesters()");

        long count = repository.count();

        log.info("[OK] RegistrationService.getNumberOfRegesters() — Total registrations: {}", count);
        return Result.success((int) count);
    }

    @Override
    public Result<Integer> getNumberOfRegestersForEvent(int eventId) {
        log.info("[START] RegistrationService.getNumberOfRegestersForEvent() — eventId={}", eventId);

        long count = repository.countByEventId(eventId);

        log.info("[OK] RegistrationService.getNumberOfRegestersForEvent() — Total registrations for event {}: {}",
                eventId, count);
        return Result.success((int) count);
    }

    @Override
    public Result<BigDecimal> getTotalSpents(int userId) {
        List<Registration> registrations = repository.findByUserId(userId)
                .stream()
                .filter(r -> r.getRegistrationStatus() == RegistrationStatus.PAID)
                .toList();

        BigDecimal totalSpents = BigDecimal.ZERO;
        for (Registration r : registrations) {
            Event event = eventRepository.findById(r.getEventId()).orElse(null);
            if (event != null && event.getPrice() != null) {
                totalSpents = totalSpents.add(event.getPrice());
            }
        }

        return Result.success(totalSpents);
    }
}
