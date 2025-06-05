package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@Schema(name = "LoginRequest", description = "Payload for user login (email + password).")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(description = "User's registered email", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Schema(description = "Plain‐text password", example = "P@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
