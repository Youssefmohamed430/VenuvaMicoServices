package com.example.notif_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDto {
    private int registrationId;
    private int userId;
    private String userName;
    private String userEmail;
    private int eventId;
    private String eventTitle;
}
