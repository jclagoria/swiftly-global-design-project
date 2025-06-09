package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "UserCreationResponse",
        description = "Sent back after successful user registration"
)
public class UserCreationResponse {

    @Schema(
            description = "A brief confirmation message",
            example = "User registered successfully"
    )
    private String message;

    @Schema(
            description = "ID of the newly created user",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    )
    private UUID userId;
}