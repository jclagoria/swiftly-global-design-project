package com.swiftly.service.user.application.service;


import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService.login()")
public class UserServiceLoginTest {

    @Mock
    UserEntityRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    @DisplayName("returns token on valid creds")
    void valid() {
        var email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        var pwd   = TestFixtures.rnd.nextObject(String.class);
        var userEnt = TestFixtures.aEntity()
                .withEmail(email)
                .withPasswordHash("HASHED")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(userEnt));
        when(passwordEncoder.matches(pwd, "HASHED")).thenReturn(true);
        when(jwtTokenProvider.createToken(email)).thenReturn("TOK");

        StepVerifier.create(userService.login(TestFixtures.aLoginRequest(email, pwd)))
                .expectNext("TOK")
                .verifyComplete();
    }

    @Test
    @DisplayName("errors if user not found")
    void noUser() {
        var email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        var pwd   = TestFixtures.rnd.nextObject(String.class);

        when(userRepository.findByEmail(email)).thenReturn(Mono.empty());

        StepVerifier.create(userService.login(TestFixtures.aLoginRequest(email, pwd)))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }
}
