package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginResponse", description = "JWT issued after successful authentication.")
public class LoginResponse {

    @Schema(
            description = "JWT access token. Prefix with 'Bearer ' when sending in Authorization header.",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9…"
    )
    private String token;

    private String refreshToken;

}
