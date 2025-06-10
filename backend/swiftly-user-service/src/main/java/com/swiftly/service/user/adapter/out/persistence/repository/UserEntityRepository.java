package com.swiftly.service.user.adapter.out.persistence.repository;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository interface for performing reactive database operations on the UserEntity.
 *
 * Extends R2dbcRepository to provide standard CRUD functionality.
 * The entity type is UserEntity and the ID type is UUID.
 */
public interface UserEntityRepository extends R2dbcRepository<UserEntity, UUID> {
    /**
     * Check if a user exists with the given email.
     *
     * @param email the email to check
     * @return a Mono that emits a boolean indicating whether the user exists
     */
    Mono<Boolean> existsByEmail(String email);


    /**
     * Retrieve a user by their ID.
     *
     * @param id the ID of the user to retrieve
     * @return a Mono that emits the matching UserEntity
     */
    Mono<UserEntity> findByIdAndDeletedIsFalse(UUID id);

    /**
     * Retrieve a user by their email.
     *
     * @param email the email to query by
     * @return a Mono that emits the matching UserEntity
     */
    Mono<UserEntity> findByEmailAndDeletedIsFalse(String email);
}

