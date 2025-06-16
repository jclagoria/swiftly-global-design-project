package com.swiftly.service.user.adapter.out.persistence.repository;

import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenRepository extends R2dbcRepository<RefreshTokenEntity, UUID> {

    /**
     * Finds a refresh token entity by its token value, ensuring it has not been revoked.
     *
     * @param token the token value to search for
     * @return a Mono emitting the matching RefreshTokenEntity, or an empty Mono if no match is found
     */
    Mono<RefreshTokenEntity> findByTokenAndRevokedFalse(UUID token);

    /**
     * Deletes all refresh tokens that have expired before the given cut-off time.
     *
     * @param cutOff the cut-off time before which refresh tokens should be deleted
     * @return a Mono emitting a void value, indicating the deletion was successful
     */
    Mono<Void> deleteByExpiresAtBefore(Instant cutOff);

}
