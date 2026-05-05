package com.example.registration_service.Controllers;

import com.example.AOP.Annotation.HandleException;
import com.example.AOP.Annotation.Loggable;
import com.example.registration_service.Config.ResponseUtility;
import com.example.registration_service.RegisterationDto.CancleRegisrationDto;
import com.example.registration_service.RegisterationDto.RegistrationRequestDto;
import com.example.registration_service.Services.IRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RegistrationController {

    private final IRegistrationService registrationService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ATTENDEE')")
    @HandleException
    @Loggable(value = "RegisterUser", logArguments = true, logResult = false)
    public ResponseEntity<?> register(@RequestBody RegistrationRequestDto requestDto) {
        log.info("[ATTENDEE] RegistrationController.register() — userId={}, eventId={}",
                requestDto.getUserId(), requestDto.getEventId());
        var result = registrationService.registerUserToEvent(requestDto);
        return ResponseUtility.toResponse(result, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ATTENDEE') or hasRole('ADMIN')")
    @HandleException
    @Loggable(value = "GetUserRegistrations", logArguments = true, logResult = false)
    public ResponseEntity<?> getUserRegistrations(@PathVariable int userId) {
        log.info("[USER] RegistrationController.getUserRegistrations() — userId={}", userId);
        var result = registrationService.getUserRegistrations(userId);
        return ResponseUtility.toResponse(result);
    }

    @DeleteMapping("/cancel")
    @PreAuthorize("hasRole('ATTENDEE')")
    @HandleException
    @Loggable(value = "CancelRegistration", logArguments = true, logResult = false)
    public ResponseEntity<?> cancelRegistration(@RequestBody CancleRegisrationDto dto) {
        log.info("[ATTENDEE] RegistrationController.cancelRegistration() — userId={}, eventId={}",
                dto.getUserId(), dto.getEventId());
        var result = registrationService.cancelRegistration(dto);
        if (result.isSuccess()) {
            return ResponseEntity.ok(new MessageResponse("You are cancelled successfully"));
        }
        return ResponseUtility.toResponse(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getNumberOfRegesters")
    @HandleException
    @Loggable(value = "GetNumberOfRegisters", logArguments = false, logResult = false)
    public ResponseEntity<?> getNumberOfRegesters() {
        log.info("[ADMIN] RegistrationController.getNumberOfRegesters()");
        var result = registrationService.getNumberOfRegesters();
        return ResponseUtility.toResponse(result);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER')")
    @GetMapping("/getNumberOfRegestersForEvent/{eventId}")
    @HandleException
    @Loggable(value = "GetNumberOfRegistersForEvent", logArguments = true, logResult = false)
    public ResponseEntity<?> getNumberOfRegestersForEvent(@PathVariable int eventId) {
        log.info("[ADMIN/ORGANIZER] RegistrationController.getNumberOfRegestersForEvent() — eventId={}", eventId);
        var result = registrationService.getNumberOfRegestersForEvent(eventId);
        return ResponseUtility.toResponse(result);
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @GetMapping("/getTotalSpents/{userId}")
    @HandleException
    @Loggable(value = "GetTotalSpents", logArguments = true, logResult = false)
    public ResponseEntity<?> getTotalSpents(@PathVariable int userId) {
        log.info("[ATTENDEE] RegistrationController.getTotalSpents() — userId={}", userId);
        var result = registrationService.getTotalSpents(userId);
        return ResponseUtility.toResponse(result);
    }

    public record MessageResponse(String message) {
    }
}
