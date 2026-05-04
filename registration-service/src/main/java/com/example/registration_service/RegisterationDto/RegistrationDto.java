package com.example.registration_service.RegisterationDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDto {

    private int registrationId;

    private int userId;
    private String userName;
    private String userEmail;

    private int eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;
    private boolean paymentRequired;

    private String status;
    private String paymentStatus;
    private LocalDateTime registeredAt;
}
