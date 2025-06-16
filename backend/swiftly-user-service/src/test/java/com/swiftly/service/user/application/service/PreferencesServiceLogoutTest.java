package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.RevokedTokenEntity;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.data.TestFixtures;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreferencesServiceLogoutTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    R2dbcEntityTemplate r2dbcTemplate;

    @InjectMocks
    AuthServiceImpl authService;

    private AutoCloseable mocksTest;

    @BeforeEach
    void setUp() {
        mocksTest = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksTest != null) {
            mocksTest.close();
        }
    }

    @Test
    @DisplayName("inserts revoked token on valid token")
    void success() {
        var token = TestFixtures.rnd.nextObject(UUID.class).toString();
        var exp = TestFixtures.rnd.nextObject(Date.class);
        Claims claims = mock(Claims.class);

        when(claims.getExpiration()).thenReturn(exp);
        when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);

        var insert = mock(ReactiveInsertOperation.ReactiveInsert.class);
        when(r2dbcTemplate.insert(RevokedTokenEntity.class)).thenReturn(insert);
        when(insert.using(any(RevokedTokenEntity.class))).thenReturn(Mono.empty());

        StepVerifier.create(authService.logout(token))
                .verifyComplete();
    }

    @Test
    @DisplayName("propagates DB errors")
    void dbError() {
        var token  = TestFixtures.rnd.nextObject(String.class);
        var exp    = new Date(System.currentTimeMillis() + 1_000_000);
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(exp);
        when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);

        var insert = mock(ReactiveInsertOperation.ReactiveInsert.class);
        when(r2dbcTemplate.insert(RevokedTokenEntity.class)).thenReturn(insert);
        when(insert.using(any(RevokedTokenEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("fail")));

        StepVerifier.create(authService.logout(token))
                .expectErrorMessage("fail")
                .verify();
    }

}
