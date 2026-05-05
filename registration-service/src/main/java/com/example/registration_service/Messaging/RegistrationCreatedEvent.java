package com.example.registration_service.Messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published by Registration Service when a new registration is created.
 * Notification Service listens for this to send a registration confirmation notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationCreatedEvent {
    private int userId;
    private String userName;
    private int eventId;
    private String eventTitle;
    private String message;
}
