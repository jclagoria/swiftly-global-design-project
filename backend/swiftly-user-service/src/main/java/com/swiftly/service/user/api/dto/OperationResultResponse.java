package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name        = "OperationResultResponse",
        description = "Standard response for successful mutation operations"
)
public class OperationResultResponse {
    @Schema(description = "Operation result code", example = "UPDATE_OK")
    private String code;

    @Schema(description = "Human-readable message", example = "User updated successfully")
    private String message;
}