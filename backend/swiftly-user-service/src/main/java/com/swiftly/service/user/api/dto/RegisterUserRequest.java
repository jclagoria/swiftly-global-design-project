package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
@Schema(
        name = "RegisterUserRequest",
        description = "Payload for registering a new user (email, password, names, and optional profile details)."
)
public class RegisterUserRequest {

    @NotBlank @Email
    @Schema(
            description = "Email address to register",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank
    @Schema(
            description = "Plain-text password for the new account",
            example = "P@ssw0rd",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    @NotBlank
    @Schema(
            description = "User's first name",
            example = "John",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String firstName;

    @NotBlank
    @Schema(
            description = "User's last name",
            example = "Doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String lastName;

    @Schema(
            description = "User's phone number (optional)",
            example = "+1-234-567-8901",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String phone;

    @Schema(
            description = "User's postal address (optional)",
            example = "123 Main St, Springfield, IL 62704",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String address;

    @Schema(
            description = "Locale code for the user (optional)",
            example = "en-US",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String locale;

    @Schema(
            description = "IANA Time Zone identifier for the user (optional)",
            example = "America/New_York",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String timezone;

}
