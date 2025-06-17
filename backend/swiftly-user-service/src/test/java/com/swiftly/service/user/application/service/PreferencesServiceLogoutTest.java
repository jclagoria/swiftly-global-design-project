package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PreferencesServiceLogoutTest {

    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RevokedTokenRepository revokedTokenRepository;

    // The other collaborators are not used by logout(), but are required by constructor
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock Clock clock;

    @InjectMocks AuthServiceImpl authService;

    @Test
    @DisplayName("inserts revoked token on valid token")
    void success() {
        var token = TestFixtures.rnd.nextObject(UUID.class).toString();
        var expDate = TestFixtures.rnd.nextObject(Date.class);
        Claims claims = mock(Claims.class);

        when(claims.getExpiration()).thenReturn(expDate);
        when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);
        when(revokedTokenRepository.insert(any(RevokedTokenEntity.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(authService.logout(token))
                .verifyComplete();

        // verify that insert was called with the right entity
        ArgumentCaptor<RevokedTokenEntity> captor = ArgumentCaptor.forClass(RevokedTokenEntity.class);
        verify(revokedTokenRepository).insert(captor.capture());
        RevokedTokenEntity passed = captor.getValue();
        assert passed.getToken().equals(token);
        assert passed.getExpiresAt().equals(expDate.toInstant());
    }

    @Test
    @DisplayName("propagates DB errors")
    void dbError() {
        var token  = TestFixtures.rnd.nextObject(String.class);
        var expDate    = new Date(System.currentTimeMillis() + 1_000_000);
        Claims claims = mock(Claims.class);

        when(claims.getExpiration()).thenReturn(expDate);
        when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);
        when(revokedTokenRepository.insert(any(RevokedTokenEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("DB failure")));

        StepVerifier.create(authService.logout(token))
                .expectErrorMessage("DB failure")
                .verify();
    }

    @Test
    @DisplayName("invalid token maps to InvalidCredentialsException")
    void invalidToken() {
        String badToken = "not-a-jwt";
        when(jwtTokenProvider.parseClaims(badToken))
                .thenThrow(new RuntimeException("parse error"));

        StepVerifier.create(authService.logout(badToken))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

}
