package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name        = "UpdateUserRequest",
        description = "Payload to update a user's core and profile details"
)
public class UpdateUserRequest {

    @NotBlank
    @Schema(description = "New first name", example = "Jane", required = true)
    private String firstName;

    @NotBlank
    @Schema(description = "New last name", example = "Doe", required = true)
    private String lastName;

    @Schema(description = "New phone number", example = "+541112345678")
    private String phone;

    @Schema(description = "New address", example = "123 Main St, Buenos Aires")
    private String address;

    @Schema(description = "New locale", example = "es-AR")
    private String locale;

    @Schema(description = "New time zone", example = "America/Argentina/Buenos_Aires")
    private String timezone;

}
