package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService-register()")
public class UserServiceRegisterTest {

    @Mock UserEntityRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserPersistenceMapper mapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    @DisplayName("throws when email already exists")
    void alreadyExists() {
        var req = TestFixtures.aRegisterRequest();
        when(userRepository.existsByEmail(req.getEmail()))
                .thenReturn(Mono.just(true));

        StepVerifier.create(userService.register(req))
                .expectError(EmailAlreadyInUseException.class)
                .verify();
    }

    @Test
    @DisplayName("saves and returns model when email is new")
    void success() {
        var req = TestFixtures.aRegisterRequest();
        var toSave = TestFixtures.aEntity()
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();
        var saved = TestFixtures.aEntity()
                .withId(UUID.randomUUID())
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();
        var expected = TestFixtures.aDomainModel()
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(req.getPassword())).thenReturn("ENCODED");
        when(mapper.toEntity(any())).thenReturn(toSave);
        when(userRepository.save(any())).thenReturn(Mono.just(saved));
        when(mapper.toDomain(saved)).thenReturn(expected);

        StepVerifier.create(userService.register(req))
                .expectNext(expected)
                .verifyComplete();

        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("ENCODED")));
    }

    @Test
    @DisplayName("errors when user not found")
    void userNotFound() {
        var email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        var pwd = TestFixtures.rnd.nextObject(String.class);

        when(userRepository.findByEmail(email)).thenReturn(Mono.empty());

        StepVerifier.create(userService.login(TestFixtures.aLoginRequest(email, pwd)))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

}
