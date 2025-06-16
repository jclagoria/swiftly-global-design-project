package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.api.dto.RefreshTokenRequest;
import com.swiftly.service.user.api.dto.RefreshTokenResponse;
import com.swiftly.service.user.application.port.in.AuthService;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserServiceUtils userServiceUtils;
    private final RefreshTokenRepository refreshTokenRepository;
    private final R2dbcEntityTemplate r2dbcTemplate;

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        return userRepository.findByEmailAndDeletedIsFalse(request.getEmail())
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new InvalidCredentialsException());
                    }
                    String jwtToken = jwtTokenProvider.createToken(user.getEmail());
                    log.info("User logged in successfully: {}", user.getEmail());

                    RefreshTokenEntity refreshToken = userServiceUtils.createRefreshTokenEntity(user.getId());
                    return refreshTokenRepository.save(refreshToken)
                            .map(refreshTokenEntity -> {
                                log.info("Refresh token created for user: {}", user.getEmail());
                                return new LoginResponse(jwtToken, refreshTokenEntity.getToken().toString());
                            });
                })
                .doOnSuccess(response -> log.info("User {} logged in successfully", request.getEmail()))
                .doOnError(err -> {
                    if (err instanceof InvalidCredentialsException) {
                        log.warn("Invalid credentials for email {}", request.getEmail());
                    } else {
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
                .doOnError(err -> log.error("Error inserting revoked token: {}", err.getMessage()))
                // Return a Mono emitting a void value, indicating the logout was successful
                .then(); // return Mono<Void>
    }

    @Override
    public Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        UUID refreshTokenId = UUID.fromString(request.getRefreshToken());
        return refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenId)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .flatMap(rt -> {
                    if (rt.getExpiresAt().isBefore(Instant.now())) {
                        return Mono.error(new InvalidCredentialsException());
                    }
                    rt.setRevoked(true);
                    return refreshTokenRepository.save(rt)
                            .then(Mono.just(rt));
                }).flatMap(rt -> {
                    String newJWT = jwtTokenProvider.createToken(rt.getUserId().toString());
                    Instant now = Instant.now();
                    Instant expiry = now.plus(Duration.ofDays(15));
                    RefreshTokenEntity refreshTokenResponse =
                            new RefreshTokenEntity(rt.getUserId(), now, expiry, false);

                    return refreshTokenRepository.save(refreshTokenResponse)
                            .map(refreshTokenEntity -> {
                                log.info("New refresh token created for user: {}", rt.getUserId());
                                return new RefreshTokenResponse(newJWT, refreshTokenEntity.getToken().toString());
                            });
                });
    }
}
