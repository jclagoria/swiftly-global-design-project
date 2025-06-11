package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name        = "ChangePasswordRequest",
        description = "Payload for changing the current user’s password"
)
public class ChangePasswordRequest {

    @NotBlank
    @Size(min = 8)
    @Schema(description = "Current password", example = "P@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank
    @Size(min = 8)
    @Schema(description = "New password", example = "P@ssw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

}
