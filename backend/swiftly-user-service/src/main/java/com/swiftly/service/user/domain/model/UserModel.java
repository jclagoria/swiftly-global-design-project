package com.swiftly.service.user.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representation of a user returned after successful registration or fetch.")
public class UserModel {

    @Schema(description = "Unique identifier for the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @JsonIgnore
    private String passwordHash;

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "Timestamp when the user was created (ISO 8601)", example = "2025-06-02T15:23:01.123Z")
    private Instant createdAt;

    @JsonIgnore
    private Boolean deleted;

    @JsonIgnore
    private Instant deletedAt;
}
