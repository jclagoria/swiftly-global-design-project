package com.swiftly.service.user.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Encoders;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private EasyRandom easyRandom;
    private JwtTokenProvider provider;
    private String secret;
    private static final long EXPIRATION_SECONDS = 3600;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();
        provider = new JwtTokenProvider();

        // generate secure random 256-bit secret for HS256
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(keyBytes);
        secret = Encoders.BASE64.encode(keyBytes);

        // inject secret and expiration into provider
        TestUtils.setField(provider, "jwtSecret", secret);
        TestUtils.setField(provider, "jwtExpiration", EXPIRATION_SECONDS);
        provider.init();
    }

    // Helper to create a token for a given subject with optional custom expiration
    private String createToken(String subject, long expirationSeconds) {
        TestUtils.setField(provider, "jwtExpiration", expirationSeconds);
        return provider.createToken(subject);
    }

    @Nested
    class CreateTokenTests {
        @Test
        void shouldGenerateNonNullToken() {
            String subject = easyRandom.nextObject(String.class);
            String token = provider.createToken(subject);
            assertNotNull(token, "Token should not be null");
            assertFalse(token.isBlank(), "Token should not be blank");
        }

        @Test
        void generatedTokenShouldContainSubject() {
            String subject = easyRandom.nextObject(String.class);
            String token = provider.createToken(subject);
            String extracted = provider.getSubject(token);
            assertEquals(subject, extracted, "Subject extracted should match original");
        }
    }

    @Nested
    class ValidateTokenTests {
        @Test
        void shouldValidateValidToken() {
            String subject = easyRandom.nextObject(String.class);
            String token = provider.createToken(subject);
            assertTrue(provider.validateToken(token), "Valid token should pass validation");
        }

        @Test
        void shouldRejectExpiredToken() {
            String subject = easyRandom.nextObject(String.class);
            String token = createToken(subject, -1);
            assertFalse(provider.validateToken(token), "Expired token should fail validation");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"abc.def.ghi", "not-a-token"})
        void shouldRejectMalformedOrEmptyToken(String token) {
            assertFalse(provider.validateToken(token), "Malformed or empty token should fail validation");
        }
    }

    @Nested
    class ParseClaimsTests {
        @Test
        void shouldParseClaimsFromValidToken() {
            String subject = easyRandom.nextObject(String.class);
            String token = provider.createToken(subject);
            Claims claims = provider.parseClaims(token);
            assertNotNull(claims.getIssuedAt(), "IssuedAt should be set");
            assertNotNull(claims.getExpiration(), "Expiration should be set");
            assertEquals(subject, claims.getSubject(), "Parsed subject should match");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowIllegalArgumentForEmptyToken(String token) {
            assertThrows(IllegalArgumentException.class,
                    () -> provider.parseClaims(token),
                    "Parsing an empty or null token should throw IllegalArgumentException");
        }

        @ParameterizedTest
        @ValueSource(strings = {"xxx.yyy.zzz"})
        void shouldThrowJwtExceptionForMalformedToken(String token) {
            assertThrows(JwtException.class,
                    () -> provider.parseClaims(token),
                    "Parsing a malformed token should throw JwtException");
        }
    }

    @Nested
    class GetSubjectTests {
        @Test
        void shouldReturnCorrectSubject() {
            String subject = easyRandom.nextObject(String.class);
            String token = provider.createToken(subject);
            String parsed = provider.getSubject(token);
            assertEquals(subject, parsed, "getSubject should return correct subject");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void shouldThrowIllegalArgumentForEmptySubjectToken(String token) {
            assertThrows(IllegalArgumentException.class,
                    () -> provider.getSubject(token),
                    "getSubject on empty or null token should throw IllegalArgumentException");
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid.token.here"})
        void shouldThrowJwtExceptionForMalformedSubjectToken(String token) {
            assertThrows(JwtException.class,
                    () -> provider.getSubject(token),
                    "getSubject on malformed token should throw JwtException");
        }
    }

    /**
     * Utility for reflection-based field injection in tests.
     */
    static class TestUtils {
        static void setField(Object target, String fieldName, Object value) {
            try {
                var field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}