package com.example.notif_service.Abstractions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Error {
    public static final Error NONE = new Error("");

    private final String message;

    public Error(String code, String message) {
        this.message = message;
    }

}

