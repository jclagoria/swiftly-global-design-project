package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.mongo.UserPreferencesEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPreferencesMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.mongo.UserPreferencesRepository;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.UserPreferencesNotFoundException;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
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
@DisplayName("UserService-getUserPreferences()")
public class UserServiceGetUserPreferencesTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserPreferencesMapper preferencesMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("throws when preferences not found")
    void throwsWhenPreferencesNotFound() {
        UUID userId = UUID.randomUUID();

        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserPreferences(userId))
                .expectErrorMatches(ex ->
                        ex instanceof UserPreferencesNotFoundException &&
                                ex.getMessage().contains(userId.toString()))
                .verify();

        verify(userPreferencesRepository).findByUserId(userId);
        verifyNoInteractions(preferencesMapper);
    }

    @Test
    @DisplayName("returns preferences when found")
    void returnsPreferencesWhenFound() {
        // Arrange: generate random entity via TestFixtures
        var builder = TestFixtures.randomUserPreferencesBuilder();
        UserPreferencesEntity entity = builder.buildEntity();
        UUID userId = entity.getUserId();

        // Manually construct expected model matching entity fields
        UserPreferencesModel expectedModel = UserPreferencesModel.builder()
                .userId(userId)
                .language(entity.getLanguage())
                .timezone(entity.getTimezone())
                .defaultCurrency(entity.getDefaultCurrency())
                .notifications(UserPreferencesModel.NotificationsModel.builder()
                        .email(entity.getNotifications().isEmail())
                        .sms(entity.getNotifications().isSms())
                        .push(entity.getNotifications().isPush())
                        .inApp(entity.getNotifications().isInApp())
                        .build())
                .preferences(UserPreferencesModel.PreferencesModel.builder()
                        .dailyReport(entity.getPreferences().isDailyReport())
                        .fraudAlerts(entity.getPreferences().isFraudAlerts())
                        .maxTransactionAmt(entity.getPreferences().getMaxTransactionAmt())
                        .build())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Stub repository and mapper
        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.just(entity));
        when(preferencesMapper.toDomain(entity))
                .thenReturn(expectedModel);

        // Act & Assert
        StepVerifier.create(userService.getUserPreferences(userId))
                .expectNext(expectedModel)
                .verifyComplete();

        verify(userPreferencesRepository).findByUserId(userId);
        verify(preferencesMapper).toDomain(entity);
    }

    @Test
    @DisplayName("throws when mapped data is invalid")
    void throwsWhenDataInvalid() {
        // Arrange: generate a valid-looking entity…
        var builder = TestFixtures.randomUserPreferencesBuilder();
        UserPreferencesEntity entity = builder.buildEntity();
        UUID userId = entity.getUserId();

        // Stub repository to return it…
        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.just(entity));
        // …but have the mapper blow up on bad data
        when(preferencesMapper.toDomain(entity))
                .thenThrow(new IllegalArgumentException("Corrupt preferences for user " + userId));

        // Act & Assert
        StepVerifier.create(userService.getUserPreferences(userId))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalArgumentException &&
                                ex.getMessage().contains("Corrupt preferences"))
                .verify();

        verify(userPreferencesRepository).findByUserId(userId);
        verify(preferencesMapper).toDomain(entity);
    }
}
