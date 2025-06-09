package com.swiftly.service.user.data;

import com.swiftly.service.user.domain.model.UserProfileModel;
import lombok.Builder;
import lombok.With;
import org.jeasy.random.EasyRandom;

import java.time.Instant;
import java.util.UUID;

@Builder
public class UserProfileModelBuilder {
    private static final EasyRandom easyRandom = new EasyRandom();

    @With
    @Builder.Default
    private UUID id = easyRandom.nextObject(UUID.class);

    @With @Builder.Default
    private String email = easyRandom.nextObject(String.class) + "@test.com";

    @With @Builder.Default
    private String firstName = easyRandom.nextObject(String.class);

    @With @Builder.Default
    private String lastName = easyRandom.nextObject(String.class);

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

    public UserProfileModel build() {
        return new UserProfileModel(id, email, firstName, lastName, phone, address, locale, timezone, createdAt, updatedAt);
    }

    public static UserProfileModelBuilder random() {
        return easyRandom.nextObject(UserProfileModelBuilder.class);
    }
}
