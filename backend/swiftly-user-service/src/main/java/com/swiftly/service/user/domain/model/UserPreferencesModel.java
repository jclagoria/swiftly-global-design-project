package com.swiftly.service.user.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesModel {

    private UUID userId;
    private String language;
    private String timezone;
    private String defaultCurrency;
    private NotificationsModel notifications;
    private PreferencesModel preferences;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class NotificationsModel {
        private boolean email;
        private boolean sms;
        private boolean push;
        private boolean inApp;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class PreferencesModel {
        private boolean dailyReport;
        private boolean fraudAlerts;
        private Double  maxTransactionAmt;
    }

}
