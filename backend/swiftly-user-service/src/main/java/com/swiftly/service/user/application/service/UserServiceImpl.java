package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserProfileModelMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import com.swiftly.service.user.domain.model.UserModel;
import com.swiftly.service.user.domain.model.UserProfileModel;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPersistenceMapper userMapper;
    private final UserProfileModelMapper profileMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserProfileRepository userProfileRepository;

    private final R2dbcEntityTemplate r2dbcTemplate;

    /**
     * Registers a new user with the provided details.
     *
     * @param request the registration request containing user details
     * @return a Mono emitting the registered UserModel
     */
    @Override
    public Mono<UserModel> register(RegisterUserRequest request) {
        // Attempt to find an existing user by email
        // If the user already exists, return an error
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Attempt to register with already used email: {}", request.getEmail());
                        return Mono.error(new EmailAlreadyInUseException(request.getEmail()));
                    }
                    // Otherwise, create a new user with the provided details
                    var user = userMapper.toEntity(UserModel.builder()
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
                .map(userMapper::toDomain);
    }

    /**
     * Authenticates a user and generates a JWT token upon successful login.
     *
     * @param request the login request containing user credentials
     * @return a Mono emitting the JWT token if login is successful, or an error if login fails
     */
    @Override
    public Mono<String> login(LoginRequest request) {
        // Attempt to find the user by email
        return userRepository.findByEmail(request.getEmail())
                // If no user is found, return an InvalidCredentialsException
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .flatMap(user -> {
                    // Check if the provided password matches the stored password hash
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

    /**
     * Logs out an existing user based on the provided JWT token.
     *
     * @param token the JWT token to log out
     * @return a Mono emitting a void value, indicating the logout was successful
     */
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
                .doOnError(err -> log.error("Error inserting revoked token: {}", err.getMessage()))
                // Return a Mono emitting a void value, indicating the logout was successful
                .then(); // return Mono<Void>
    }

    @Override
    public Mono<UserProfileModel> getUserProfile(UUID userId) {
        return userRepository.findById(userId)                                    // 1️⃣ fetch user
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .map(userMapper::toDomain)                                             // 2️⃣ map to UserModel
                .flatMap(userModel ->
                        userProfileRepository.findById(userId)                             // 3️⃣ fetch profile
                                .defaultIfEmpty(new UserProfileEntity(
                                        userId, null, null, "en-US", "UTC", null, null))
                                .map(profileEntity ->
                                        profileMapper.toModel(userModel, profileEntity)            // 4️⃣ combine them
                                )
                );
    }
}
