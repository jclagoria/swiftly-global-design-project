package com.swiftly.service.user.api.dto;

import java.util.List;

public record ValidationErrorResponse(
        String timestamp,
        int status,
        String error,
        List<FieldErrorDto> errors
) { }
