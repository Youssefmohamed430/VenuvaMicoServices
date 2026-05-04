package com.example.registration_service.RegisterationDto;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancleRegisrationDto {

    @Positive(message = "User ID must be positive")
    private int userId;

    @Positive(message = "Event ID must be positive")
    private int eventId;
}
