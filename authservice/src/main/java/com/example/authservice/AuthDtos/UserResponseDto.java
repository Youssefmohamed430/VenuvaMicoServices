package com.example.authservice.AuthDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private int id;
    private String name;
    private String email;
    private String role;
}
