package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import com.swiftly.service.user.domain.model.UserModel;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPersistenceMapper mapper;
    private final JwtTokenProvider jwtTokenProvider;

    private final R2dbcEntityTemplate r2dbcTemplate;

    @Override
    public Mono<UserModel> register(RegisterUserRequest request) {
        // Attempt to find an existing user by email
        return userRepository.existsByEmail(request.getEmail())
                // If the user already exists, return an error
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Attempt to register with already used email: {}", request.getEmail());
                        return Mono.error(new EmailAlreadyInUseException(request.getEmail()));
                    }
                    // Otherwise, create a new user with the provided details
                    var user = mapper.toEntity(UserModel.builder()
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .build()
                    );
                    // Save the new user to the database
                    return userRepository
                            .save(user);
                })
                // Log the result of the registration attempt
                .doOnSuccess(savedUser ->
                        log.info("User registered successfully: {}", savedUser.getEmail()))
                .doOnError(error ->
                        log.error("Error registering user for email {}: {}", request.getEmail(), error.getMessage()))
                // Map the saved user to the domain model
                .map(mapper::toDomain);
    }

    @Override
    public Mono<String> login(LoginRequest request) {
        // Attempt to find the user by email
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new InvalidCredentialsException());
                    }
                    // If the passwords match, create a JWT token for the user and log it
                    String token = jwtTokenProvider.createToken(user.getEmail());
                    log.info("User logged in successfully: {}", user.getEmail());
                    // Return the JWT token
                    return Mono.just(token);
                })
                // If the login is successful, log the success
                .doOnSuccess(token -> log.info("User {} logged in successfully", request.getEmail()))
                // If the login fails, log the error
                .doOnError(err -> {
                    if (err instanceof InvalidCredentialsException) {
                        // If the error is an InvalidCredentialsException, log a warning
                        log.warn("Invalid credentials for email {}", request.getEmail());
                    } else {
                        // If the error is any other type, log an error
                        log.error("Unexpected error during login for {}: {}", request.getEmail(), err.getMessage());
                    }
                });
    }

    @Override
    public Mono<Void> logout(String token) {
        // Parse the JWT token to extract the expiration time
        Claims claims = jwtTokenProvider.parseClaims(token);
        Instant expirationAt = claims.getExpiration().toInstant();

        // Create a revoked token entity with the parsed expiration time
        RevokedTokenEntity revokedTokenEntity = new RevokedTokenEntity(token, expirationAt);

        // Insert the revoked token into the database using the R2dbcTemplate
        return r2dbcTemplate
                .insert(RevokedTokenEntity.class)
                .using(revokedTokenEntity)
                // Log the result of the insertion attempt
                .doOnSuccess(saved -> log.info("Inserted revoked token: {}", token))
                .doOnError(err    -> log.error("Error inserting revoked token: {}", err.getMessage()))
                // Return a Mono emitting a void value, indicating the logout was successful
                .then(); // return Mono<Void>
    }
}
