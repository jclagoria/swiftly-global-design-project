package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name        = "UpdateUserPreferencesRequest",
        description = "Payload to update a user's preferences"
)
public class UpdateUserPreferencesRequest {

    @NotNull
    @Schema(example = "en-US", description = "Locale code")
    private String language;

    @NotNull
    @Schema(example = "America/Argentina/Buenos_Aires", description = "Time zone")
    private String timezone;

    @NotNull
    @Schema(example = "USD", description = "Default currency")
    private String defaultCurrency;

    @NotNull
    private Notifications notifications;

    @NotNull
    private Preferences preferences;

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "Which channels to notify on")
    public static class Notifications {
        private boolean email;
        private boolean sms;
        private boolean push;
        private boolean inApp;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    @Schema(description = "Additional preference flags")
    public static class Preferences {
        private boolean dailyReport;
        private boolean fraudAlerts;
        private Double  maxTransactionAmt;
    }


}
