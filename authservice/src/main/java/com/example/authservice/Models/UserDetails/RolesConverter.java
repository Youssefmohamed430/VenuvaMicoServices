package com.example.authservice.Models.UserDetails;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RolesConverter implements AttributeConverter<Roles, String> {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public String convertToDatabaseColumn(Roles attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Roles convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        String normalized = dbData.startsWith(ROLE_PREFIX)
                ? dbData.substring(ROLE_PREFIX.length())
                : dbData;

        return Roles.valueOf(normalized);
    }
}
