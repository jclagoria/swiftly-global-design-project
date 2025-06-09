package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserProfileMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
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
    @Mock UserProfileMapper profileMapper;
    @Mock UserProfileRepository userProfileRepository;
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

        when(userRepository.existsByEmail(req.getEmail()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(req.getPassword()))
                .thenReturn("ENCODED");
        when(mapper.toEntity(any()))
                .thenReturn(toSave);
        when(userRepository.save(any()))
                .thenReturn(Mono.just(saved));

        // —— New stubs for profile insertion ——
        var profileEntity = TestFixtures.aProfileEntity()
                .withUserId(saved.getId())
                .build();
        when(profileMapper.toEntity(saved.getId(), req))
                .thenReturn(profileEntity);
        when(userProfileRepository.insert(profileEntity))
                .thenReturn(Mono.empty());
        // —— end new stubs ——

        when(mapper.toDomain(saved))
                .thenReturn(expected);

        StepVerifier.create(userService.register(req))
                .expectNext(expected)
                .verifyComplete();

        verify(userRepository)
                .save(argThat(u -> u.getPasswordHash().equals("ENCODED")));
        // verify profile insertion
        verify(userProfileRepository)
                .insert(argThat(p -> p.getUserId().equals(saved.getId())));
    }

    @Test
    @DisplayName("propagates error when profile insertion fails")
    void profileInsertionFails() {
        // given
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

        // stubbing the “new email” path
        when(userRepository.existsByEmail(req.getEmail()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(req.getPassword()))
                .thenReturn("ENCODED");
        when(mapper.toEntity(any()))
                .thenReturn(toSave);
        when(userRepository.save(any()))
                .thenReturn(Mono.just(saved));

        // stub profile‐mapper
        var profileEntity = TestFixtures.aProfileEntity()
                .withUserId(saved.getId())
                .build();
        when(profileMapper.toEntity(saved.getId(), req))
                .thenReturn(profileEntity);

        // simulate failure on profile insert
        when(userProfileRepository.insert(profileEntity))
                .thenReturn(Mono.error(new RuntimeException("Insert failed")));

        // when / then
        StepVerifier.create(userService.register(req))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                "Insert failed".equals(ex.getMessage())
                )
                .verify();

        // (optional) verify that we did call the mapper before the insert
        verify(mapper).toEntity(argThat(model ->
                model.getEmail().equals(req.getEmail()) &&
                        model.getPasswordHash().equals("ENCODED") &&
                        model.getFirstName().equals(req.getFirstName()) &&
                        model.getLastName().equals(req.getLastName())
        ));
        verify(profileMapper).toEntity(saved.getId(), req);
    }

    @Test
    @DisplayName("errors when password does not match")
    void invalidPassword() {
        // given
        var email = TestFixtures.rnd.nextObject(String.class) + "@x.com";
        var rawPwd = TestFixtures.rnd.nextObject(String.class);
        var encodedHash = "SOME_HASH";

        var storedUser = TestFixtures.aEntity()
                .withEmail(email)
                .withPasswordHash(encodedHash)
                .build();

        // stub finding the user successfully
        when(userRepository.findByEmail(email))
                .thenReturn(Mono.just(storedUser));

        // stub password check to fail
        when(passwordEncoder.matches(rawPwd, encodedHash))
                .thenReturn(false);

        // when / then
        StepVerifier.create(userService.login(TestFixtures.aLoginRequest(email, rawPwd)))
                .expectError(InvalidCredentialsException.class)
                .verify();

        // verify we actually checked the password
        verify(passwordEncoder).matches(rawPwd, encodedHash);
    }

}
