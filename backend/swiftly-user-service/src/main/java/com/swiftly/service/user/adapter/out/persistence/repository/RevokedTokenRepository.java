package com.swiftly.service.user.adapter.out.persistence.repository;

import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

/**
 * Repository interface for performing database operations on revoked tokens.
 *
 * Extends R2dbcRepository to inherit standard CRUD operations.
 * The entity type is RevokedTokenEntity and the ID type is String.
 */
public interface RevokedTokenRepository extends R2dbcRepository<RevokedTokenEntity, String> {

    /**
     * Check if a revoked token exists in the database with the given token value.
     *
     * @param token the value of the token to check
     * @return a Mono that emits a boolean indicating whether the token exists
     */
    Mono<Boolean> existsByToken(String token);

    /**
     * Inserts a new revoked token into the database.
     *
     * @param revokedTokenEntity the RevokedTokenEntity to be inserted
     * @return a Mono emitting a void value, indicating the insertion was successful
     */
    @Query("""
        INSERT INTO revoked_tokens(token, expires_at)
          VALUES (:#{#p.token},
                  :#{#p.expiresAt}
                 )
    """)
    Mono<Void> insert(@Param("p") RevokedTokenEntity revokedTokenEntity);

}
