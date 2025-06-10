package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
import com.swiftly.service.user.api.dto.UpdateUserRequest;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService-updateUserProfile()")
public class UserServiceUpdateUserProfileTest {

    @Mock
    UserEntityRepository userRepository;

    @Mock
    UserProfileRepository userProfileRepository;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    @DisplayName("throws when user not found")
    void userNotFound() {
        UUID userId = UUID.randomUUID();
        // request content does not matter here, use random data
        UpdateUserRequest req = TestFixtures.randomUpdateRequest().build();

        when(userRepository.findByIdAndDeletedIsFalse(userId)).thenReturn(Mono.empty());
        when(userProfileRepository.findById(userId))
                .thenReturn(Mono.just(TestFixtures.randomProfileEntity()));

        StepVerifier.create(userService.updateUserProfile(userId, req))
                .expectErrorMatches(ex ->
                        ex instanceof UserNotFoundException
                                && ex.getMessage().contains(userId.toString()))
                .verify();

        verify(userRepository).findByIdAndDeletedIsFalse(userId);
        verifyNoMoreInteractions(userProfileRepository);
    }

    @Test
    @DisplayName("completes without calling save when profile not found")
    void noOpWhenProfileMissing() {
        UUID userId = UUID.randomUUID();
        UserEntity user = TestFixtures.randomEntity();
        // only identity matters, random data is fine
        UpdateUserRequest req = TestFixtures.randomUpdateRequest().build();

        when(userRepository.findByIdAndDeletedIsFalse(userId)).thenReturn(Mono.just(user));
        when(userProfileRepository.findById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userService.updateUserProfile(userId, req))
                .verifyComplete();

        verify(userRepository).findByIdAndDeletedIsFalse(userId);
        verify(userProfileRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("patches user and all profile fields when present")
    void patchesAllFields() {
        UUID userId = UUID.randomUUID();
        UserEntity user = TestFixtures.randomEntity();
        UserProfileEntity profile = TestFixtures.randomProfileEntity();
        UpdateUserRequest req = TestFixtures.aUpdateRequest()
                .build();

        when(userRepository.findByIdAndDeletedIsFalse(userId)).thenReturn(Mono.just(user));
        when(userProfileRepository.findById(userId)).thenReturn(Mono.just(profile));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userProfileRepository.save(any(UserProfileEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(userService.updateUserProfile(userId, req))
                .verifyComplete();

        verify(userRepository).save(argThat(u ->
                req.getFirstName().equals(u.getFirstName()) &&
                        req.getLastName().equals(u.getLastName())
        ));
        verify(userProfileRepository).save(argThat(p ->
                req.getPhone().equals(p.getPhone()) &&
                        req.getAddress().equals(p.getAddress()) &&
                        req.getLocale().equals(p.getLocale()) &&
                        req.getTimezone().equals(p.getTimezone()) &&
                        p.getUpdatedAt() != null
        ));
    }

    @Test
    @DisplayName("only patches provided profile fields, leaves others untouched")
    void patchesSelectiveFields() {
        UUID userId = UUID.randomUUID();
        UserEntity user = TestFixtures.randomEntity();
        UserProfileEntity profile = TestFixtures.randomProfileEntity();

        // capture original values
        String originalPhone  = profile.getPhone();
        String originalLocale = profile.getLocale();

        // only address & timezone provided
        UpdateUserRequest req = UpdateUserRequest.builder()
                .address("NewAddr")
                .timezone("Europe/London")
                .build();

        when(userRepository.findByIdAndDeletedIsFalse(userId)).thenReturn(Mono.just(user));
        when(userProfileRepository.findById(userId)).thenReturn(Mono.just(profile));
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userProfileRepository.save(any(UserProfileEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(userService.updateUserProfile(userId, req))
                .verifyComplete();

        // only address & timezone changed, phone/locale unchanged
        verify(userProfileRepository).save(argThat(p ->
                "NewAddr".equals(p.getAddress()) &&
                        "Europe/London".equals(p.getTimezone()) &&
                        originalPhone.equals(p.getPhone()) &&
                        originalLocale.equals(p.getLocale())
        ));
    }
}
