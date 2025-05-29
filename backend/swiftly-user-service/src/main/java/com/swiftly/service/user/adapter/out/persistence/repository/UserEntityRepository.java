package com.swiftly.service.user.adapter.out.persistence.repository;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserEntityRepository extends R2dbcRepository<UserEntity, UUID> {
    Mono<Boolean> existsByEmail(String email);
}
