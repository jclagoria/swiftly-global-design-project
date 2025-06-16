package com.swiftly.service.user.application.service;


import com.swiftly.service.user.adapter.out.persistence.entities.RefreshTokenEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.RefreshTokenRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService.login()")
public class UserServiceLoginTest {

    @Mock
    UserEntityRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    @DisplayName("returns token on valid creds")
    void valid() {
        // Arrange
        var email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        var pwd   = TestFixtures.rnd.nextObject(String.class);

        UserEntity userEnt = TestFixtures.aEntity()
                .withEmail(email)
                .withPasswordHash("HASHED")
                .build();

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(userEnt));
        when(passwordEncoder.matches(pwd, "HASHED"))
                .thenReturn(true);
        when(jwtTokenProvider.createToken(email))
                .thenReturn("TOK");

        // Stub the saved RefreshTokenEntity to return a known token
        UUID refreshToken = UUID.randomUUID();
        RefreshTokenEntity savedEntity = mock(RefreshTokenEntity.class);
        when(savedEntity.getToken()).thenReturn(refreshToken);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(Mono.just(savedEntity));

        LoginResponse expected = new LoginResponse(
                "TOK",
                refreshToken.toString()
        );

        // Act & Assert
        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, pwd)
                ))
                .expectNext(expected)   // uses Lombok @Data equals()
                .verifyComplete();
    }

    @Test
    @DisplayName("errors if user not found")
    void noUser() {
        var email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        var pwd   = TestFixtures.rnd.nextObject(String.class);

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, pwd)
                ))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("errors if password mismatch")
    void invalidPassword() {
        String email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        String pwd   = TestFixtures.rnd.nextObject(String.class);

        UserEntity userEnt = TestFixtures.aEntity()
                .withEmail(email)
                .withPasswordHash("HASHED")
                .build();

        when(userRepository.findByEmailAndDeletedIsFalse(email))
                .thenReturn(Mono.just(userEnt));
        when(passwordEncoder.matches(pwd, "HASHED"))
                .thenReturn(false);

        StepVerifier.create(userService.login(
                        TestFixtures.aLoginRequest(email, pwd)
                ))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }
}
