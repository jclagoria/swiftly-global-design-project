package com.swiftly.service.user.config.security;

import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtReactiveAuthenticationManagerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private JwtReactiveAuthenticationManager authManager;

    @BeforeEach
    void setUp() {
        authManager = new JwtReactiveAuthenticationManager(jwtTokenProvider, revokedTokenRepository);
    }

    // Helper to build a BearerTokenAuthenticationToken
    private BearerTokenAuthenticationToken bearer(String token) {
        return new BearerTokenAuthenticationToken(token);
    }

    // Helper to perform authentication call
    private Mono<Authentication> authenticate(String token) {
        return authManager.authenticate(bearer(token));
    }

    // Helper to assert BadCredentialsException with message containing snippet
    private void assertBadCredentials(String messageSnippet, Mono<Authentication> mono) {
        StepVerifier.create(mono)
                .expectErrorSatisfies(error -> {
                    assertInstanceOf(BadCredentialsException.class, error,
                            "Expected BadCredentialsException");
                    assertTrue(error.getMessage().contains(messageSnippet),
                            "Error message should contain: " + messageSnippet);
                })
                .verify();
    }

    @Nested
    @DisplayName("When authentication type is not BearerTokenAuthenticationToken")
    class AuthenticationTypeValidationTests {
        @Test
        void shouldRejectNonBearerToken() {
            Mono<Authentication> mono = authManager.authenticate(mock(Authentication.class));

            StepVerifier.create(mono)
                    .expectErrorMatches(throwable ->
                            throwable instanceof BadCredentialsException &&
                                    throwable.getMessage().equals("No Bearer token in request")
                    )
                    .verify();
        }
    }

    @Nested
    @DisplayName("When JWT parsing fails or token is invalid/expired")
    class JwtParsingTests {
        @ParameterizedTest(name = "should reject invalid JWT: {0}")
        @ValueSource(strings = {"Expired token", "Malformed token"})
        void shouldRejectInvalidOrExpiredJwt(String exceptionMessage) {
            String token = "dummy-token";
            when(jwtTokenProvider.parseClaims(token))
                    .thenThrow(new JwtException(exceptionMessage));

            Mono<Authentication> mono = authenticate(token);
            assertBadCredentials("JWT invalid or expired: " + exceptionMessage, mono);
        }
    }

    @Nested
    @DisplayName("When token has been revoked")
    class RevokedTokenTests {
        @Test
        void shouldRejectRevokedToken() {
            String token = "revoked-token";
            Claims claims = mock(Claims.class);

            when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);
            when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(true));

            assertBadCredentials("JWT has been revoked", authenticate(token));
        }
    }

    @Nested
    @DisplayName("When token is valid and not revoked")
    class SuccessfulAuthenticationTests {
        @Test
        void shouldAuthenticateValidToken() {
            String token = "valid-token";
            Claims claims = mock(Claims.class);
            String subject = "user123";

            when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(subject);
            when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));

            StepVerifier.create(authenticate(token))
                    .assertNext(auth -> {
                        assertInstanceOf(UsernamePasswordAuthenticationToken.class,
                                auth,
                                "Authentication should be UsernamePasswordAuthenticationToken");
                        UsernamePasswordAuthenticationToken upt = (UsernamePasswordAuthenticationToken) auth;
                        assertEquals(subject, upt.getPrincipal());
                        assertNull(upt.getCredentials());
                        assertTrue(upt.getAuthorities().isEmpty(),
                                "Authorities should be empty");
                    })
                    .verifyComplete();
        }
    }

}