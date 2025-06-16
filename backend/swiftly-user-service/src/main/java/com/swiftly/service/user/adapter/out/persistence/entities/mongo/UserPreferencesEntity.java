package com.swiftly.service.user.adapter.out.persistence.entities.mongo;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "user_preferences")
public class UserPreferencesEntity {

    @MongoId
    private String id;

    private UUID userId;

    private String language;
    private String timezone;
    private String defaultCurrency;

    private Notifications notifications;
    private Preferences preferences;

    private Instant createdAt;
    private Instant updatedAt;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Notifications {
        private boolean email;
        private boolean sms;
        private boolean push;
        private boolean inApp;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Preferences {
        private boolean dailyReport;
        private boolean fraudAlerts;
        private Double  maxTransactionAmt;
    }
}
