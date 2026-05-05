package com.example.notif_service.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNotificationDto {

    @NotNull(message = "User ID is required")
    private int userId;

    @NotNull(message = "Event ID is required")
    private int eventId;

    @NotBlank(message = "Message is required")
    private String message;
}
