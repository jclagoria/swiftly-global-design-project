package com.swiftly.service.user.data;

import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import lombok.Builder;
import lombok.With;
import org.jeasy.random.EasyRandom;

import java.time.Instant;
import java.util.UUID;

@Builder
public class UserProfileEntityBuilder {
    private static final EasyRandom easyRandom = new EasyRandom();

    @With
    @Builder.Default
    private UUID userId = easyRandom.nextObject(UUID.class);

    @With @Builder.Default
    private String phone = easyRandom.nextObject(String.class);

    @With @Builder.Default
    private String address = easyRandom.nextObject(String.class);

    @With @Builder.Default
    private String locale = easyRandom.nextObject(String.class);

    @With @Builder.Default
    private String timezone = easyRandom.nextObject(String.class);

    @Builder.Default
    private Instant createdAt = easyRandom.nextObject(Instant.class);

    @Builder.Default
    private Instant updatedAt = easyRandom.nextObject(Instant.class);

    public UserProfileEntity build() {
        return new UserProfileEntity(userId, phone, address, locale, timezone, createdAt, updatedAt);
    }

    public static UserProfileEntityBuilder random() {
        return easyRandom.nextObject(UserProfileEntityBuilder.class);
    }
}
