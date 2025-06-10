package com.swiftly.service.user.data;

import com.swiftly.service.user.domain.model.UserModel;
import lombok.Builder;
import lombok.With;
import org.jeasy.random.EasyRandom;

import java.time.Instant;
import java.util.UUID;

@Builder
public class UserModelBuilder {

    private static final EasyRandom easyRandom = new EasyRandom();

    @Builder.Default
    private UUID id = easyRandom.nextObject(UUID.class);

    @With
    @Builder.Default
    private String email = easyRandom.nextObject(String.class) + "@test.com";

    @With
    @Builder.Default
    private String passwordHash = easyRandom.nextObject(String.class);

    @Builder.Default
    private String firstName = easyRandom.nextObject(String.class);

    @Builder.Default
    private String lastName = easyRandom.nextObject(String.class);

    @Builder.Default
    private Instant createdAt = easyRandom.nextObject(Instant.class);

    @Builder.Default
    private Boolean deleted = easyRandom.nextObject(Boolean.class);

    @Builder.Default
    private Instant deletedAt = easyRandom.nextObject(Instant.class);

    public UserModel build() {
        return new UserModel(id, email, passwordHash, firstName,
                lastName, createdAt, deleted, deletedAt);
    }

    /**
     * Convenience method to create a random UserModelBuilder.
     * @return a random UserModelBuilder
     */
    public static UserModelBuilder random() {
        return easyRandom
                .nextObject(UserModelBuilder.class);
    }
}
