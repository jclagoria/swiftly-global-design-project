package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserProfileMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService-getUserProfile()")
public class UserServiceGetUserProfileTest {

    @Mock
    UserEntityRepository userRepository;
    @Mock
    UserProfileRepository userProfileRepository;
    @Mock
    UserPersistenceMapper userMapper;
    @Mock
    UserProfileMapper profileMapper;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    @DisplayName("throws when user not found")
    void userNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserProfile(userId))
                .expectErrorMatches(throwable -> throwable instanceof UserNotFoundException
                        && throwable.getMessage().contains(userId.toString()))
                .verify();
    }

    @Test
    @DisplayName("returns combined model when profile exists")
    void profileExists() {
        UUID userId = UUID.randomUUID();
        var userEntity = TestFixtures.randomEntity();
        var userModel = TestFixtures.randomModel();
        var profileEntity = TestFixtures.randomProfileEntity();
        var expectedModel = TestFixtures.randomProfileModel();

        when(userRepository.findById(userId)).thenReturn(Mono.just(userEntity));
        when(userMapper.toDomain(userEntity)).thenReturn(userModel);
        when(userProfileRepository.findById(userId)).thenReturn(Mono.just(profileEntity));
        when(profileMapper.toModel(userModel, profileEntity)).thenReturn(expectedModel);

        StepVerifier.create(userService.getUserProfile(userId))
                .expectNext(expectedModel)
                .verifyComplete();

        verify(userProfileRepository).findById(userId);
    }

    @Test
    @DisplayName("returns default profile when none exists")
    void defaultProfile() {
        UUID userId = UUID.randomUUID();
        var userEntity = TestFixtures.randomEntity();
        var userModel = TestFixtures.randomModel();
        var defaultEntity = new UserProfileEntity(
                userId, null, null, "en-US", "UTC", null, null);
        var expectedModel = TestFixtures.randomProfileModel();

        when(userRepository.findById(userId)).thenReturn(Mono.just(userEntity));
        when(userMapper.toDomain(userEntity)).thenReturn(userModel);
        when(userProfileRepository.findById(userId)).thenReturn(Mono.empty());
        when(profileMapper.toModel(userModel, defaultEntity)).thenReturn(expectedModel);

        StepVerifier.create(userService.getUserProfile(userId))
                .expectNext(expectedModel)
                .verifyComplete();
    }

}
