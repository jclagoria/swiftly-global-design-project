package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserProfileMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService-register()")
public class PreferencesServiceRegisterTest {

    @Mock
    UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserPersistenceMapper mapper;
    @Mock UserProfileMapper profileMapper;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserServiceUtils userServiceUtils;              // ← NEW
    @InjectMocks UserManagementServiceImpl userManagementService;

    @Test
    @DisplayName("throws when email already exists")
    void alreadyExists() {
        var req = TestFixtures.aRegisterRequest();

        // stub util.validateEmailExists instead of repo.existsByEmail
        when(userServiceUtils.validateEmailExists(req.getEmail(), userRepository))
                .thenReturn(Mono.just(true));

        StepVerifier.create(userManagementService.register(req))
                .expectError(EmailAlreadyInUseException.class)
                .verify();

        verify(userServiceUtils).validateEmailExists(req.getEmail(), userRepository);
    }

    @Test
    @DisplayName("saves and returns model when email is new")
    void success() {
        var req = TestFixtures.aRegisterRequest();

        // prepare entities/models
        var toSave = TestFixtures.aEntity()
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();
        var saved = TestFixtures.aEntity()
                .withId(java.util.UUID.randomUUID())
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();
        var expected = TestFixtures.aDomainModel()
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();

        // 1) email check
        when(userServiceUtils.validateEmailExists(req.getEmail(), userRepository))
                .thenReturn(Mono.just(false));
        // 2) password hashing
        when(passwordEncoder.encode(req.getPassword()))
                .thenReturn("ENCODED");
        // 3) util creates entity
        when(userServiceUtils.createUserEntity(req))
                .thenReturn(toSave);
        // 4) repository save
        when(userRepository.save(toSave))
                .thenReturn(Mono.just(saved));

        // profile insert stubs (unchanged)
        var profileEntity = TestFixtures.aProfileEntity()
                .withUserId(saved.getId())
                .build();
        when(profileMapper.toEntity(saved.getId(), req))
                .thenReturn(profileEntity);
        when(userProfileRepository.insert(profileEntity))
                .thenReturn(Mono.empty());

        // mapping to domain
        when(mapper.toDomain(saved))
                .thenReturn(expected);

        StepVerifier.create(userManagementService.register(req))
                .expectNext(expected)
                .verifyComplete();

        // verify we called through util, repo, and profile insertion
        verify(userServiceUtils).createUserEntity(req);
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("ENCODED")));
        verify(userProfileRepository).insert(argThat(p -> p.getUserId().equals(saved.getId())));
    }

    @Test
    @DisplayName("propagates error when profile insertion fails")
    void profileInsertionFails() {
        var req = TestFixtures.aRegisterRequest();
        var toSave = TestFixtures.aEntity()
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();
        var saved = TestFixtures.aEntity()
                .withId(java.util.UUID.randomUUID())
                .withEmail(req.getEmail())
                .withPasswordHash("ENCODED")
                .build();

        when(userServiceUtils.validateEmailExists(req.getEmail(), userRepository))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(req.getPassword()))
                .thenReturn("ENCODED");
        when(userServiceUtils.createUserEntity(req))
                .thenReturn(toSave);
        when(userRepository.save(toSave))
                .thenReturn(Mono.just(saved));

        var profileEntity = TestFixtures.aProfileEntity()
                .withUserId(saved.getId())
                .build();
        when(profileMapper.toEntity(saved.getId(), req))
                .thenReturn(profileEntity);
        when(userProfileRepository.insert(profileEntity))
                .thenReturn(Mono.error(new RuntimeException("Insert failed")));

        StepVerifier.create(userManagementService.register(req))
                .expectErrorMessage("Insert failed")
                .verify();

        verify(profileMapper).toEntity(saved.getId(), req);
    }

}
