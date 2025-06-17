package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.api.dto.RefreshTokenRequest;
import com.swiftly.service.user.api.dto.RefreshTokenResponse;
import com.swiftly.service.user.application.port.in.AuthService;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final Clock clock;

    @Value("${jwt.refresh-token-ttl-days}")
    private long REFRESH_TOKEN_EXPIRATION;

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        Mono<UserEntity> userMono = userRepository.findByEmailAndDeletedIsFalse(request.getEmail());
        if (userMono == null) {
            userMono = Mono.empty();
        }

        return userMono
                // 2) if we got a user, validate their password & build tokens
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new InvalidCredentialsException());
                    }

                    // subject is always the userId
                    String jwtToken = jwtTokenProvider.createToken(user.getId().toString());

                    Instant now    = Instant.now(clock);
                    Instant expiry = now.plus(Duration.ofDays(REFRESH_TOKEN_EXPIRATION));
                    RefreshTokenEntity refreshEntity =
                            new RefreshTokenEntity(user.getId(), now, expiry, false);

                    return refreshTokenRepository.save(refreshEntity)
                            .map(rt -> {
                                log.info("Refresh token created for userId={}", user.getId());
                                return new LoginResponse(jwtToken, rt.getToken().toString());
                            });
                })
                // 3) if flatMap never fired (i.e. no user at all), turn it into InvalidCredentials
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                // 4) shared logging logic
                .doOnError(err -> {
                    if (err instanceof InvalidCredentialsException) {
                        log.warn("Invalid login attempt for email={}", request.getEmail());
                    } else {
                        log.error("Unexpected login error for email={}: {}", request.getEmail(), err.getMessage());
                    }
                });
    }

    @Override
    public Mono<Void> logout(String token) {
        return Mono.fromCallable(() -> jwtTokenProvider.parseClaims(token))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(ex -> new InvalidCredentialsException())
                .flatMap(claims -> {
                    Instant expiresAt = claims.getExpiration().toInstant();
                    RevokedTokenEntity revoked = new RevokedTokenEntity(token, expiresAt);
                    return revokedTokenRepository.insert(revoked)
                            .doOnSuccess(__ -> log.info("Revoked token persisted for token={}", token))
                            .doOnError(err -> log.error("Failed to persist revoked token: {}", err.getMessage()))
                            .then();
                });
    }

    @Transactional
    @Override
    public Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        return Mono.fromCallable(() -> UUID.fromString(request.getRefreshToken()))
                .onErrorMap(IllegalArgumentException.class, ex -> new InvalidCredentialsException())

                // Fetch the non-revoked refresh token
                .flatMap(id -> refreshTokenRepository
                        .findByTokenAndRevokedFalse(id)
                        .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                )

                // Revoke the old token
                .flatMap(rt -> {
                    if (rt.getExpiresAt().isBefore(Instant.now(clock))) {
                        return Mono.error(new InvalidCredentialsException());
                    }
                    rt.setRevoked(true);
                    return refreshTokenRepository.save(rt).thenReturn(rt);
                })

                // Issue and persist the new refresh token + JWT
                .flatMap(oldRt -> {
                    String newJwt = jwtTokenProvider.createToken(oldRt.getUserId().toString());
                    Instant now = Instant.now(clock);
                    Instant expiry = now.plus(Duration.ofDays(REFRESH_TOKEN_EXPIRATION));
                    RefreshTokenEntity newRt =
                            new RefreshTokenEntity(oldRt.getUserId(), now, expiry, false);

                    return refreshTokenRepository.save(newRt)
                            .map(saved -> {
                                log.info("New refresh token created for userId={}", oldRt.getUserId());
                                return new RefreshTokenResponse(newJwt, saved.getToken().toString());
                            });
                });
    }
}
