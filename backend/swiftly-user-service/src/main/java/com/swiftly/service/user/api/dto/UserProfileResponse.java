package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name="UserProfileResponse", description="User profile data")
public class UserProfileResponse {

    @Schema(description="User ID", example="3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID userId;

    @Schema(description="Email address", example="user@example.com")
    private String email;

    @Schema(description="First name", example="John")
    private String firstName;

    @Schema(description="Last name", example="Doe")
    private String lastName;

    @Schema(description="Account created at (ISO 8601)", example="2025-06-02T15:23:01.123Z")
    private Instant createdAt;

    @Schema(description="Phone number", example="+1234567890")
    private String phone;

    @Schema(description="Address", example="123 Main St, City")
    private String address;

    @Schema(description="Locale", example="es-AR")
    private String locale;

    @Schema(description="Time zone", example="America/Argentina/Buenos_Aires")
    private String timezone;

    @Schema(description="Profile last updated at (ISO 8601)", example="2025-06-05T10:00:00.000Z")
    private Instant updatedAt;


}
