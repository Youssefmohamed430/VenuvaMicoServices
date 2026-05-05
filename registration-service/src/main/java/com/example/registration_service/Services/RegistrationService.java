package com.example.registration_service.Services;

import com.example.AOP.Annotation.Loggable;
import com.example.registration_service.Abstractions.Error;
import com.example.registration_service.Abstractions.Result;
import com.example.registration_service.Client.AuthServiceClient;
import com.example.registration_service.Client.EventServiceClient;
import com.example.registration_service.DTOs.EventDto;
import com.example.registration_service.DTOs.UserDto;
import com.example.registration_service.Messaging.RegistrationCreatedEvent;
import com.example.registration_service.Messaging.RegistrationPublisher;
import com.example.registration_service.Models.Registration;
import com.example.registration_service.RegisterationDto.CancleRegisrationDto;
import com.example.registration_service.RegisterationDto.RegistrationDto;
import com.example.registration_service.RegisterationDto.RegistrationRequestDto;
import com.example.registration_service.RegistrationStatus;
import com.example.registration_service.Repos.RegistrationRepository;
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

    // ===== Inter-Service Clients (Sync REST) =====
    private final AuthServiceClient authClient;    // → Auth Service
    private final EventServiceClient eventClient;  // → Event Service

    // ===== Message Publisher (Async RabbitMQ) =====
    private final RegistrationPublisher registrationPublisher;

    @Override
    public boolean isUserAlreadyRegistered(int userId, int eventId) {
        return repository.existsByUserIdAndEventId(userId, eventId);
    }

    @Override
    @Loggable(value = "RegisterUserToEvent", logArguments = true, logResult = false)
    public Result<RegistrationDto> registerUserToEvent(RegistrationRequestDto requestDto) {
        log.info("[START] RegistrationService.registerUserToEvent() — userId={}, eventId={}",
                requestDto.getUserId(), requestDto.getEventId());

        // 1. Check duplicate registration (local DB)
        if (isUserAlreadyRegistered(requestDto.getUserId(), requestDto.getEventId())) {
            log.warn("[WARN] RegistrationService.registerUserToEvent() — User {} already registered for event {}",
                    requestDto.getUserId(), requestDto.getEventId());
            return Result.failure(new Error("User already registered for this event"));
        }

        // 2. Fetch event from Event Service (Sync REST call)
        EventDto event = eventClient.getEventById(requestDto.getEventId());
        if (event == null) {
            log.warn("[WARN] RegistrationService.registerUserToEvent() — Event not found via Event Service: {}",
                    requestDto.getEventId());
            return Result.failure(new Error("Event not found"));
        }

        // 3. Fetch user from Auth Service (Sync REST call)
        UserDto user = authClient.getUserById(requestDto.getUserId());
        if (user == null) {
            log.warn("[WARN] RegistrationService.registerUserToEvent() — User not found via Auth Service: {}",
                    requestDto.getUserId());
            return Result.failure(new Error("User not found"));
        }

        // 4. Create registration (local DB only)
        Registration registration = new Registration();
        registration.setUserId(requestDto.getUserId());
        registration.setEventId(requestDto.getEventId());
        registration.setRegistrationStatus(
            event.isPaymentRequired() ? RegistrationStatus.PENDING : RegistrationStatus.PAID
        );
        repository.save(registration);

        // 5. Build response DTO
        RegistrationDto dto = new RegistrationDto();
        dto.setRegistrationId(registration.getId());
        dto.setUserId(user.getId());
        dto.setUserName(user.getName());
        dto.setUserEmail(user.getEmail());
        dto.setEventId(event.getId());
        dto.setEventTitle(event.getTitle());
        dto.setEventDate(event.getDate());
        dto.setEventLocation(event.getLocation());
        dto.setPaymentRequired(event.isPaymentRequired());
        dto.setStatus(registration.getRegistrationStatus().toString());

        // 6. Publish registration.created (Async RabbitMQ) — fire-and-forget
        try {
            String message = "You are registered for: " + event.getTitle();
            registrationPublisher.publishRegistrationCreated(
                new RegistrationCreatedEvent(
                    user.getId(),
                    user.getName(),
                    event.getId(),
                    event.getTitle(),
                    message
                )
            );
        } catch (Exception e) {
            // Don't fail the registration if notification publishing fails
            log.warn("[WARN] RegistrationService.registerUserToEvent() — Failed to publish registration.created: {}", e.getMessage());
        }

        log.info("[OK] RegistrationService.registerUserToEvent() — User {} registered to event {}",
                requestDto.getUserId(), requestDto.getEventId());
        return Result.success(dto);
    }

    @Override
    @Loggable(value = "CancelRegistration", logArguments = true, logResult = false)
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
            return Result.failure(new Error("User is not registered for this event"));
        }

        repository.delete(registration);
        log.info("[OK] RegistrationService.cancelRegistration() — User {} unregistered from event {}",
                dto.getUserId(), dto.getEventId());
        return Result.success(true);
    }

    @Override
    @Loggable(value = "GetUserRegistrations", logArguments = true, logResult = false)
    public Result<List<RegistrationDto>> getUserRegistrations(int userId) {
        log.info("[START] RegistrationService.getUserRegistrations() — userId={}", userId);

        List<Registration> registrations = repository.findByUserId(userId);
        if (registrations == null || registrations.isEmpty()) {
            log.warn("[WARN] RegistrationService.getUserRegistrations() — No registrations found for userId={}", userId);
            return Result.failure(new Error("No registrations found"));
        }

        List<RegistrationDto> result = registrations.stream().map(r -> {
            RegistrationDto regDto = new RegistrationDto();
            regDto.setRegistrationId(r.getId());
            regDto.setUserId(r.getUserId());

            // Fetch event from Event Service (Sync REST call)
            EventDto event = eventClient.getEventById(r.getEventId());
            if (event != null) {
                regDto.setEventId(event.getId());
                regDto.setEventTitle(event.getTitle());
                regDto.setEventDate(event.getDate());
                regDto.setEventLocation(event.getLocation());
                regDto.setPaymentRequired(event.isPaymentRequired());
            } else {
                regDto.setEventId(r.getEventId());
                regDto.setEventTitle("Event unavailable");
            }

            regDto.setStatus(r.getRegistrationStatus().toString());
            return regDto;
        }).collect(Collectors.toList());

        log.info("[OK] RegistrationService.getUserRegistrations() — {} registrations retrieved for userId={}",
                result.size(), userId);
        return Result.success(result);
    }

    @Override
    @Loggable(value = "GetNumberOfRegisters", logArguments = false, logResult = false)
    public Result<Integer> getNumberOfRegesters() {
        log.info("[START] RegistrationService.getNumberOfRegesters()");
        long count = repository.count();
        log.info("[OK] RegistrationService.getNumberOfRegesters() — Total registrations: {}", count);
        return Result.success((int) count);
    }

    @Override
    @Loggable(value = "GetNumberOfRegistersForEvent", logArguments = true, logResult = false)
    public Result<Integer> getNumberOfRegestersForEvent(int eventId) {
        log.info("[START] RegistrationService.getNumberOfRegestersForEvent() — eventId={}", eventId);
        long count = repository.countByEventId(eventId);
        log.info("[OK] RegistrationService.getNumberOfRegestersForEvent() — Total: {} for event {}", count, eventId);
        return Result.success((int) count);
    }

    @Override
    @Loggable(value = "GetTotalSpents", logArguments = true, logResult = false)
    public Result<BigDecimal> getTotalSpents(int userId) {
        log.info("[START] RegistrationService.getTotalSpents() — userId={}", userId);

        List<Registration> registrations = repository.findByUserId(userId)
                .stream()
                .filter(r -> r.getRegistrationStatus() == RegistrationStatus.PAID)
                .toList();

        BigDecimal totalSpents = BigDecimal.ZERO;
        for (Registration r : registrations) {
            // Fetch event from Event Service (Sync REST call) to get price
            EventDto event = eventClient.getEventById(r.getEventId());
            if (event != null && event.getPrice() != null) {
                totalSpents = totalSpents.add(event.getPrice());
            }
        }

        log.info("[OK] RegistrationService.getTotalSpents() — Total spent: {} for userId={}", totalSpents, userId);
        return Result.success(totalSpents);
    }
}
