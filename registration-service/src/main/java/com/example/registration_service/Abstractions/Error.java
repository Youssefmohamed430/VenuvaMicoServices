package com.example.registration_service.Abstractions;

import lombok.Getter;

@Getter
public class Error {
    public static final Error NONE = new Error("");

    private final String message;

    public Error(String message) {
        this.message = message;
    }
}
