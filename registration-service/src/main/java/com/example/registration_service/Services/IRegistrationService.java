package com.example.registration_service.Services;

import com.example.registration_service.Abstractions.Result;
import com.example.registration_service.RegisterationDto.CancleRegisrationDto;
import com.example.registration_service.RegisterationDto.RegistrationDto;
import com.example.registration_service.RegisterationDto.RegistrationRequestDto;

import java.math.BigDecimal;
import java.util.List;

public interface IRegistrationService {

    Result<RegistrationDto> registerUserToEvent(RegistrationRequestDto requestDto);

    Result<List<RegistrationDto>> getUserRegistrations(int userId);

    Result<Boolean> cancelRegistration(CancleRegisrationDto cancelRegistration);

    boolean isUserAlreadyRegistered(int userId, int eventId);

    Result<Integer> getNumberOfRegesters();

    Result<Integer> getNumberOfRegestersForEvent(int eventId);

    Result<BigDecimal> getTotalSpents(int userId);
}
