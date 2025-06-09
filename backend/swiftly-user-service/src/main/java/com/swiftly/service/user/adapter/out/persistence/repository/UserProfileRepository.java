package com.swiftly.service.user.adapter.out.persistence.repository;

import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserProfileRepository extends R2dbcRepository<UserProfileEntity, UUID> {

    /**
     * Inserts a new user profile into the database.
     *
     * @param userProfileEntity the UserProfileEntity to be inserted
     * @return a Mono emitting a void value, indicating the insertion was successful
     */
    @Query("""
        INSERT INTO user_profiles(user_id, phone, address, locale, timezone, updated_at)
          VALUES (:#{#p.userId},
                  :#{#p.phone},
                  :#{#p.address},
                  :#{#p.locale},
                  :#{#p.timezone}
                 )
    """)
    Mono<Void> insert(@Param("p") UserProfileEntity userProfileEntity);

}
