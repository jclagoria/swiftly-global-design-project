package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserServiceUtils {

    private final PasswordEncoder passwordEncoder;

    /**
     * Validates if a user with the given email already exists in the database.
     * 
     * @param email The email to check
     * @param userRepository The repository to query
     * @return A Mono containing true if the email exists, false otherwise
     */
    public Mono<Boolean> validateEmailExists(String email, UserRepository userRepository) {
        return userRepository.existsByEmail(email)
                .doOnNext(exists -> {
                    if (exists) {
                        log.warn("Attempt to use already registered email: {}", email);
                    }
                });
    }

    /**
     * Creates a new user entity from a registration request.
     * 
     * @param request The registration request
     * @return A new UserEntity with encoded password
     */
    /**
     * Creates a new user entity with hashed password from a registration request.
     * 
     * @param request The registration request containing user details
     * @return A new UserEntity with hashed password
     */
    public UserEntity createUserEntity(RegisterUserRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        return new UserEntity(null, request.getEmail(), hashedPassword,
                request.getFirstName(), request.getLastName(), null, false, null);
    }

    /**
     * Creates a refresh token entity for the given user.
     * 
     * @param userId The ID of the user to create a token for
     * @return A new RefreshTokenEntity
     */
    public RefreshTokenEntity createRefreshTokenEntity(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(15));
        return new RefreshTokenEntity(userId, now, expiry, false);
    }
}
