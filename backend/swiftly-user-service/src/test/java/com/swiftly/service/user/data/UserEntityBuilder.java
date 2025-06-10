package com.swiftly.service.user.data;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import lombok.Builder;
import lombok.With;
import org.jeasy.random.EasyRandom;

import java.time.Instant;
import java.util.UUID;

@Builder
public class UserEntityBuilder {

    private static final EasyRandom easyRandom = new EasyRandom();

    @With
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

    public UserEntity build() {
        return new UserEntity(id, email, passwordHash,
                firstName, lastName, createdAt, deleted, deletedAt);
    }

    /**
     * Convenience method to create a random UserEntityBuilder.
     * @return a random UserEntityBuilder
     */
    public static UserEntityBuilder random() {
        return easyRandom
                .nextObject(UserEntityBuilder.class);
    }
}
