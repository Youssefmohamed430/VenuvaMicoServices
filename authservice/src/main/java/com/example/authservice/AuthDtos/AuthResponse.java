package com.example.authservice.AuthDtos;

public class AuthResponse {
    public int Id;
    public String email;
    public String role;
    public String token;
    public String refreshToken;
    public String username;

    public AuthResponse(int id, String email, String role, String token, String refreshToken, String username) {
        this.Id = id;
        this.email = email;
        this.role = role;
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
    }
}