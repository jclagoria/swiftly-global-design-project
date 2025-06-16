package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.mongo.UserPreferencesEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPreferencesMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.mongo.UserPreferencesRepository;
import com.swiftly.service.user.api.dto.UpdateUserPreferencesRequest;
import com.swiftly.service.user.application.port.in.PreferencesService;
import com.swiftly.service.user.domain.exception.UserPreferencesNotFoundException;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class PreferencesServiceImpl implements PreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserPreferencesMapper preferencesMapper;

    @Override
    public Mono<UserPreferencesModel> getUserPreferences(UUID userId) {
        return userPreferencesRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new UserPreferencesNotFoundException(userId)))
                .map(preferencesMapper::toDomain);
    }

    @Override
    public Mono<UserPreferencesModel> updateUserPreferences(UUID userId, UpdateUserPreferencesRequest req) {
        Instant now = Instant.now();
        return userPreferencesRepository.findByUserId(userId)
                .defaultIfEmpty(new UserPreferencesEntity(null, userId, null, null,
                        null, null, null, now ,null)
                )
                .flatMap(entity -> {
                    entity.setLanguage(req.getLanguage());
                    entity.setTimezone(req.getTimezone());
                    entity.setDefaultCurrency(req.getDefaultCurrency());
                    entity.setNotifications(
                            new UserPreferencesEntity.Notifications(
                                    req.getNotifications().isEmail(),
                                    req.getNotifications().isSms(),
                                    req.getNotifications().isPush(),
                                    req.getNotifications().isInApp()
                            )
                    );
                    entity.setPreferences(
                            new UserPreferencesEntity.Preferences(
                                    req.getPreferences().isDailyReport(),
                                    req.getPreferences().isFraudAlerts(),
                                    req.getPreferences().getMaxTransactionAmt()
                            )
                    );
                    entity.setUpdatedAt(now);
                    return userPreferencesRepository.save(entity);
                })
                .map(preferencesMapper::toDomain);
    }
}
