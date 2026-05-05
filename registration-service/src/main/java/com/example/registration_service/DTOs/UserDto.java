package com.example.registration_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user data fetched from Auth Service via REST API.
 * Maps to the response of GET /api/auth/users/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private int id;
    private String name;
    private String email;
    private String role;
}
