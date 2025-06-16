package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.mongo.UserPreferencesEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPreferencesMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.mongo.UserPreferencesRepository;
import com.swiftly.service.user.api.dto.UpdateUserPreferencesRequest;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService-updateUserPreferences()")
public class UserServiceUpdatePreferencesPreferencesTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserPreferencesMapper preferencesMapper;

    @InjectMocks
    private PreferencesServiceImpl userService;

    @Captor
    private ArgumentCaptor<UserPreferencesEntity> entityCaptor;

    private UUID userId;
    private UpdateUserPreferencesRequest request;
    private Instant now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        now = Instant.now();

        // build a request with distinct values
        request = UpdateUserPreferencesRequest.builder()
                .language("es-AR")
                .timezone("America/Argentina/Buenos_Aires")
                .defaultCurrency("ARS")
                .notifications(UpdateUserPreferencesRequest.Notifications.builder()
                        .email(true)
                        .sms(false)
                        .push(true)
                        .inApp(false)
                        .build())
                .preferences(UpdateUserPreferencesRequest.Preferences.builder()
                        .dailyReport(true)
                        .fraudAlerts(false)
                        .maxTransactionAmt(Double.valueOf("1500.45"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("updates existing preferences and returns mapped model")
    void updatesExistingPreferences() {
        // existing entity from DB
        UserPreferencesEntity existing = new UserPreferencesEntity(
                UUID.randomUUID().toString(),  // entity id
                userId,
                "en-US", "UTC", "USD",
                new UserPreferencesEntity.Notifications(false, true, false, true),
                new UserPreferencesEntity.Preferences(false, true, Double.valueOf("123.45")),
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(3600)
        );

        // saved entity returned by repository.save(...)
        UserPreferencesEntity saved = new UserPreferencesEntity(
                existing.getId(),
                userId,
                request.getLanguage(), request.getTimezone(), request.getDefaultCurrency(),
                new UserPreferencesEntity.Notifications(
                        request.getNotifications().isEmail(),
                        request.getNotifications().isSms(),
                        request.getNotifications().isPush(),
                        request.getNotifications().isInApp()
                ),
                new UserPreferencesEntity.Preferences(
                        request.getPreferences().isDailyReport(),
                        request.getPreferences().isFraudAlerts(),
                        request.getPreferences().getMaxTransactionAmt()
                ),
                existing.getCreatedAt(),
                now
        );

        UserPreferencesModel expected = UserPreferencesModel.builder()
                .userId(userId)
                .language(request.getLanguage())
                .timezone(request.getTimezone())
                .defaultCurrency(request.getDefaultCurrency())
                .notifications(UserPreferencesModel.NotificationsModel.builder()
                        .email(request.getNotifications().isEmail())
                        .sms(request.getNotifications().isSms())
                        .push(request.getNotifications().isPush())
                        .inApp(request.getNotifications().isInApp())
                        .build())
                .preferences(UserPreferencesModel.PreferencesModel.builder()
                        .dailyReport(request.getPreferences().isDailyReport())
                        .fraudAlerts(request.getPreferences().isFraudAlerts())
                        .maxTransactionAmt(request.getPreferences().getMaxTransactionAmt())
                        .build())
                .createdAt(existing.getCreatedAt())
                .updatedAt(now)
                .build();

        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.just(existing));
        when(userPreferencesRepository.save(any()))
                .thenReturn(Mono.just(saved));
        when(preferencesMapper.toDomain(saved))
                .thenReturn(expected);

        StepVerifier.create(userService.updateUserPreferences(userId, request))
                .expectNext(expected)
                .verifyComplete();

        verify(userPreferencesRepository).findByUserId(userId);
        verify(userPreferencesRepository).save(entityCaptor.capture());
        verify(preferencesMapper).toDomain(saved);

        UserPreferencesEntity captured = entityCaptor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals(request.getLanguage(), captured.getLanguage());
        assertEquals(request.getTimezone(), captured.getTimezone());
        assertEquals(request.getDefaultCurrency(), captured.getDefaultCurrency());
        assertEquals(request.getNotifications().isEmail(), captured.getNotifications().isEmail());
        assertEquals(request.getPreferences().getMaxTransactionAmt(), captured.getPreferences().getMaxTransactionAmt());
    }

    @Test
    @DisplayName("creates new preferences when none exist and returns mapped model")
    void createsNewPreferencesWhenNoneExist() {
        // default entity created in service has null id, userId=userId
        // saved entity returned by repository (simulate DB assigns id)
        UserPreferencesEntity saved = new UserPreferencesEntity(
                UUID.randomUUID().toString(),
                userId,
                request.getLanguage(), request.getTimezone(), request.getDefaultCurrency(),
                new UserPreferencesEntity.Notifications(
                        request.getNotifications().isEmail(),
                        request.getNotifications().isSms(),
                        request.getNotifications().isPush(),
                        request.getNotifications().isInApp()
                ),
                new UserPreferencesEntity.Preferences(
                        request.getPreferences().isDailyReport(),
                        request.getPreferences().isFraudAlerts(),
                        request.getPreferences().getMaxTransactionAmt()
                ),
                now,
                now
        );

        UserPreferencesModel expected = UserPreferencesModel.builder()
                .userId(userId)
                .language(request.getLanguage())
                .timezone(request.getTimezone())
                .defaultCurrency(request.getDefaultCurrency())
                .notifications(UserPreferencesModel.NotificationsModel.builder()
                        .email(request.getNotifications().isEmail())
                        .sms(request.getNotifications().isSms())
                        .push(request.getNotifications().isPush())
                        .inApp(request.getNotifications().isInApp())
                        .build())
                .preferences(UserPreferencesModel.PreferencesModel.builder()
                        .dailyReport(request.getPreferences().isDailyReport())
                        .fraudAlerts(request.getPreferences().isFraudAlerts())
                        .maxTransactionAmt(request.getPreferences().getMaxTransactionAmt())
                        .build())
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.empty());
        when(userPreferencesRepository.save(any()))
                .thenReturn(Mono.just(saved));
        when(preferencesMapper.toDomain(saved))
                .thenReturn(expected);

        StepVerifier.create(userService.updateUserPreferences(userId, request))
                .expectNext(expected)
                .verifyComplete();

        verify(userPreferencesRepository).findByUserId(userId);
        verify(userPreferencesRepository).save(entityCaptor.capture());
        verify(preferencesMapper).toDomain(saved);

        UserPreferencesEntity captured = entityCaptor.getValue();
        assertNull(captured.getId());
        assertEquals(userId, captured.getUserId());
        assertEquals(request.getLanguage(), captured.getLanguage());
    }

    @Test
    @DisplayName("propagates mapper exception")
    void mapperThrows() {
        UserPreferencesEntity existing = new UserPreferencesEntity(
                UUID.randomUUID().toString(), userId,
                "x","y","z",
                null,null, now, now
        );

        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.just(existing));
        when(userPreferencesRepository.save(any()))
                .thenReturn(Mono.just(existing));
        when(preferencesMapper.toDomain(existing))
                .thenThrow(new IllegalStateException("mapping failed"));

        StepVerifier.create(userService.updateUserPreferences(userId, request))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                                && ex.getMessage().contains("mapping failed"))
                .verify();

        verify(userPreferencesRepository).findByUserId(userId);
        verify(userPreferencesRepository).save(any());
        verify(preferencesMapper).toDomain(existing);
    }

    @Test
    @DisplayName("propagates repository find error")
    void repoFindError() {
        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.error(new RuntimeException("db gone")));
        StepVerifier.create(userService.updateUserPreferences(userId, request))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().contains("db gone"))
                .verify();

        verify(userPreferencesRepository).findByUserId(userId);
        verifyNoMoreInteractions(userPreferencesRepository, preferencesMapper);
    }

    @Test
    @DisplayName("propagates repository save error")
    void repoSaveError() {
        UserPreferencesEntity existing = new UserPreferencesEntity(
                UUID.randomUUID().toString(), userId,
                "en","UTC","USD",
                null,null, now, now
        );
        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Mono.just(existing));
        when(userPreferencesRepository.save(any()))
                .thenReturn(Mono.error(new IllegalStateException("save failed")));

        StepVerifier.create(userService.updateUserPreferences(userId, request))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException
                                && ex.getMessage().contains("save failed"))
                .verify();

        verify(userPreferencesRepository).findByUserId(userId);
        verify(userPreferencesRepository).save(any());
        verifyNoInteractions(preferencesMapper);
    }

}
