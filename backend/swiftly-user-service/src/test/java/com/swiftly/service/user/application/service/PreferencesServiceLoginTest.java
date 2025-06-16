package com.swiftly.service.user.application.service;


import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService.login()")
public class PreferencesServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserServiceUtils userServiceUtils;

    @InjectMocks
    private AuthServiceImpl userService;

    @Test
    @DisplayName("returns token on valid creds")
    void valid() {
        // Arrange
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String rawPwd = TestFixtures.rnd.nextObject(String.class);
        String hashedPwd = "HASHED";
        when(passwordEncoder.encode(rawPwd)).thenReturn(hashedPwd);

        UserEntity userEnt = TestFixtures.aEntity()
                .withEmail(email)
                .withPasswordHash(hashedPwd)
                .build();

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(userEnt));
        when(passwordEncoder.matches(rawPwd, hashedPwd))
                .thenReturn(true);
        when(jwtTokenProvider.createToken(email))
                .thenReturn("TOK");

        // Stub createRefreshTokenEntity and save
        RefreshTokenEntity newRefresh = new RefreshTokenEntity(userEnt.getId(), Instant.now(),
                Instant.now().plus(Duration.ofDays(15)), false);
        when(userServiceUtils.createRefreshTokenEntity(userEnt.getId()))
                .thenReturn(newRefresh);

        UUID refreshToken = UUID.randomUUID();
        RefreshTokenEntity savedEntity = mock(RefreshTokenEntity.class);
        when(savedEntity.getToken()).thenReturn(refreshToken);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(Mono.just(savedEntity));

        LoginResponse expected = new LoginResponse("TOK", refreshToken.toString());

        // Act & Assert
        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, rawPwd)
                ))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("errors if user not found")
    void noUser() {
        // Arrange
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String pwd = TestFixtures.rnd.nextObject(String.class);

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, pwd)
                ))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("errors if password mismatch")
    void invalidPassword() {
        // Arrange
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String pwd = TestFixtures.rnd.nextObject(String.class);

        UserEntity userEnt = TestFixtures.aEntity()
                .withEmail(email)
                .withPasswordHash("HASHED")
                .build();

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(userEnt));
        when(passwordEncoder.matches(pwd, "HASHED"))
                .thenReturn(false);

        // Act & Assert
        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, pwd)
                ))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }
}
