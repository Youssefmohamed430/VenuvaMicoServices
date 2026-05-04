package com.example.authservice.AuthDtos;

import com.example.authservice.Models.UserDetails.Roles;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    public String username;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    public String email;
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    public String password;
    public Roles role;
}