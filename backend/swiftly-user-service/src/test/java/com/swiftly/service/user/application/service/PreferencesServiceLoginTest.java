package com.swiftly.service.user.application.service;


import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthServiceImpl.login()")
public class PreferencesServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RevokedTokenRepository revokedTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private Clock clock;

    @InjectMocks
    private AuthServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // set refresh-token expiry to 15 days
        ReflectionTestUtils.setField(userService, "REFRESH_TOKEN_EXPIRATION", 15L);
        // freeze time for token timestamps
        Instant fixedNow = Instant.parse("2025-06-16T00:00:00Z");
        when(clock.instant()).thenReturn(fixedNow);
    }

    @Test
    @Disabled
    @DisplayName("returns token on valid creds")
    void valid() {
        String email  = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String rawPwd = TestFixtures.rnd.nextObject(String.class);
        String hashed = "HASHED";

        // happy-path stubs
        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(
                        TestFixtures.aEntity()
                                .withEmail(email)
                                .withPasswordHash(hashed)
                                .build()
                ));
        when(passwordEncoder.matches(rawPwd, hashed)).thenReturn(true);
        when(jwtTokenProvider.createToken(anyString())).thenReturn("TOK");

        UUID refreshToken = UUID.randomUUID();
        RefreshTokenEntity savedRt = mock(RefreshTokenEntity.class);
        when(savedRt.getToken()).thenReturn(refreshToken);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(Mono.just(savedRt));

        LoginResponse expected = new LoginResponse("TOK", refreshToken.toString());

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, rawPwd)))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("errors if user not found")
    void noUser() {
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, "any")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("errors if password mismatch")
    void invalidPassword() {
        String email  = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String rawPwd = TestFixtures.rnd.nextObject(String.class);
        String hashed = "HASHED";

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(
                        TestFixtures.aEntity()
                                .withEmail(email)
                                .withPasswordHash(hashed)
                                .build()
                ));
        when(passwordEncoder.matches(rawPwd, hashed)).thenReturn(false);

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, rawPwd)))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("errors if repository returns null Mono")
    void nullUserMono() {
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(null);

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, "pwd")))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @Disabled
    @DisplayName("errors if refresh-token persistence fails")
    void refreshTokenSaveError() {
        String email  = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String rawPwd = "secret";
        String hashed = "HASHED";

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(
                        TestFixtures.aEntity()
                                .withEmail(email)
                                .withPasswordHash(hashed)
                                .build()
                ));
        when(passwordEncoder.matches(rawPwd, hashed)).thenReturn(true);
        when(jwtTokenProvider.createToken(anyString())).thenReturn("TOK");
        // simulate DB-down on save
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, rawPwd)))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "DB down".equals(ex.getMessage())
                )
                .verify();
    }

    @Test
    @Disabled
    @DisplayName("errors if userRepository itself errors")
    void userRepositoryError() {
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, "pwd")))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "DB down".equals(ex.getMessage())
                )
                .verify();
    }
}
