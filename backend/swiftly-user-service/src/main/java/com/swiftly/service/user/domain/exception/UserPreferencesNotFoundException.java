package com.swiftly.service.user.domain.exception;

import java.util.UUID;

public class UserPreferencesNotFoundException extends RuntimeException {
    public UserPreferencesNotFoundException(UUID userId) {
        super("User preferences not found for userId: " + userId);
    }
}
