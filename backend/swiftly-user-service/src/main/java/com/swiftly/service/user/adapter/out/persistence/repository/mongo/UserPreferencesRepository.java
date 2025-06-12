package com.swiftly.service.user.adapter.out.persistence.repository.mongo;

import com.swiftly.service.user.adapter.out.persistence.entities.mongo.UserPreferencesEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserPreferencesRepository extends ReactiveMongoRepository<UserPreferencesEntity, String> {

    /**
     * Retrieves a user's preferences by their user ID.
     *
     * @param userId the ID of the user whose preferences are to be retrieved
     * @return a Mono that emits the matching UserPreferencesEntity, or an empty Mono if no matching
     *  entity is found
     */
    Mono<UserPreferencesEntity> findByUserId(UUID userId);
}
