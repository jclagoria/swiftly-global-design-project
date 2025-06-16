package com.swiftly.service.user.application.service;


import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.api.dto.RefreshTokenRequest;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService-refreshToken()")
public class PreferencesServiceRefreshTokenTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("throws InvalidCredentialsException when token not found")
    void whenTokenDoesNotExist_thenInvalidCredentialsException() {
        UUID refreshTokenId = UUID.randomUUID();
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshTokenId.toString())
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenId))
                .thenReturn(Mono.empty());

        StepVerifier.create(authService.refreshToken(request))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("throws InvalidCredentialsException when token is expired")
    void whenTokenExpired_thenInvalidCredentialsException() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now().minus(Duration.ofDays(2));
        Instant expiredAt = Instant.now().minus(Duration.ofDays(1));
        RefreshTokenEntity expiredEntity = new RefreshTokenEntity(userId, createdAt, expiredAt, false);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenId))
                .thenReturn(Mono.just(expiredEntity));

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(tokenId.toString())
                .build();

        StepVerifier.create(authService.refreshToken(request))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("successfully refreshes token")
    void success() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant past = Instant.now().minus(Duration.ofDays(2));
        Instant future = Instant.now().plus(Duration.ofDays(1));
        // existing token entity (revocation)
        RefreshTokenEntity existingEntity =
                new RefreshTokenEntity(userId, past, future, false);
        existingEntity.setToken(tokenId);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenId))
                .thenReturn(Mono.just(existingEntity));

        String newJwt = "new-jwt-token";
        when(jwtTokenProvider.createToken(userId.toString()))
                .thenReturn(newJwt);

        // Simulate the “new” refresh-token saved by the repo
        UUID newRefreshTokenId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(15));
        RefreshTokenEntity newEntity =
                new RefreshTokenEntity(userId, now, expiry, false);
        newEntity.setToken(newRefreshTokenId);

        // First save() revokes old, second save() persists new
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(
                        Mono.just(existingEntity),    // revocation save
                        Mono.just(newEntity)          // new-token save
                );

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(tokenId.toString())
                .build();

        // Act & Assert
        StepVerifier.create(authService.refreshToken(request))
                .expectNextMatches(response ->
                        newJwt.equals(response.getToken()) &&
                                newRefreshTokenId.toString().equals(response.getRefreshToken())
                )
                .verifyComplete();
    }


    @Test
    @DisplayName("propagates error when revocation save fails")
    void whenRevocationSaveFails_thenError() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RefreshTokenEntity existingEntity = new RefreshTokenEntity(userId, Instant.now().minus(Duration.ofDays(1)),
                Instant.now().plus(Duration.ofDays(1)), false);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenId))
                .thenReturn(Mono.just(existingEntity));

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("revocation failure")));

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(tokenId.toString())
                .build();

        StepVerifier.create(authService.refreshToken(request))
                .expectErrorMessage("revocation failure")
                .verify();
    }

    @Test
    @DisplayName("propagates error when new token save fails")
    void whenNewTokenSaveFails_thenError() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant past = Instant.now().minus(Duration.ofDays(2));
        Instant future = Instant.now().plus(Duration.ofDays(1));
        RefreshTokenEntity existingEntity = new RefreshTokenEntity(userId, past, future, false);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(tokenId))
                .thenReturn(Mono.just(existingEntity));

        when(jwtTokenProvider.createToken(userId.toString()))
                .thenReturn("jwt-token");

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(Mono.just(existingEntity), Mono.error(new RuntimeException("new token failure")));

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(tokenId.toString())
                .build();

        StepVerifier.create(authService.refreshToken(request))
                .expectErrorMessage("new token failure")
                .verify();
    }


}
