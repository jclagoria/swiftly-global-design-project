package com.swiftly.service.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name        = "UserPreferencesResponse",
        description = "User preferences retrieved from MongoDB"
)
public class UserPreferenceResponse {

    @Schema(
            description = "User's preferred language",
            example     = "en-US"
    )
    private String language;

    @Schema(
            description = "User's preferred timezone",
            example     = "America/New_York"
    )
    private String timezone;

    @Schema(
            description = "User's default currency",
            example     = "USD"
    )
    private String defaultCurrency;

    private NotificationsResponse notifications;
    private PreferencesResponse preferences;

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NotificationsResponse {
        private boolean email;
        private boolean sms;
        private boolean push;
        private boolean inApp;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PreferencesResponse {
        private boolean dailyReport;
        private boolean fraudAlerts;
        private Double  maxTransactionAmt;
    }

}
