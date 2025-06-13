package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.UpdateUserPreferencesRequest;
import com.swiftly.service.user.api.dto.UserPreferenceResponse;
import com.swiftly.service.user.config.security.SecurityConfig;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController – PUT /{userId}/preferences")
public class UserControllerUpdatePreferencesTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserPreferencesResponseMapper responseMapper;

    @MockitoBean
    private UserProfileResponseMapper userProfileResponseMapper;

    private String token;

    @BeforeEach
    void initAuth() {
        token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
    }

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferencesTests {

        private String url(UUID userId) {
            return BASE + "/" + userId + "/preferences" ;
        }

        private WebTestClient.ResponseSpec performPut(UUID userId, UpdateUserPreferencesRequest req) {
            return webClient.put()
                    .uri(url(userId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(req)
                    .exchange();
        }

        @Test
        @DisplayName("when success, should return 200 and valid body")
        void whenSuccess_shouldReturn200() {
            UUID userId = UUID.randomUUID();

            // build the request
            UpdateUserPreferencesRequest req = new UpdateUserPreferencesRequest(
                    "es-AR",
                    "America/Argentina/Buenos_Aires",
                    "ARS",
                    new UpdateUserPreferencesRequest.Notifications(true, false, true, false),
                    new UpdateUserPreferencesRequest.Preferences(true, false, 1234.56)
            );

            // prepare domain model
            UserPreferencesModel model =  UserPreferencesModel.builder()
                    .userId(userId)
                    .language(req.getLanguage())
                    .timezone(req.getTimezone())
                    .defaultCurrency(req.getDefaultCurrency())
                    .notifications(new UserPreferencesModel.NotificationsModel(
                            req.getNotifications().isEmail(),
                            req.getNotifications().isSms(),
                            req.getNotifications().isPush(),
                            req.getNotifications().isInApp()
                    ))
                    .preferences(new UserPreferencesModel.PreferencesModel(
                            req.getPreferences().isDailyReport(),
                            req.getPreferences().isFraudAlerts(),
                            req.getPreferences().getMaxTransactionAmt()
                    ))
                    .build();

            // build a DTO with non-null fields
            UserPreferenceResponse dto = new UserPreferenceResponse(
                    model.getLanguage(),
                    model.getTimezone(),
                    model.getDefaultCurrency(),
                    new UserPreferenceResponse.NotificationsResponse(
                            model.getNotifications().isEmail(),
                            model.getNotifications().isSms(),
                            model.getNotifications().isPush(),
                            model.getNotifications().isInApp()
                    ),
                    new UserPreferenceResponse.PreferencesResponse(
                            model.getPreferences().isDailyReport(),
                            model.getPreferences().isFraudAlerts(),
                            model.getPreferences().getMaxTransactionAmt()
                    )
            );

            // *** stub with any(...) ***
            when(userService.updateUserPreferences(
                    eq(userId),
                    any(UpdateUserPreferencesRequest.class))
            )
                    .thenReturn(Mono.just(model));
            doReturn(dto)
                    .when(responseMapper)
                    .toResponse(eq(model));

            performPut(userId, req)
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.language").isEqualTo(dto.getLanguage())
                    .jsonPath("$.timezone").isEqualTo(dto.getTimezone())
                    .jsonPath("$.defaultCurrency").isEqualTo(dto.getDefaultCurrency())
                    .jsonPath("$.notifications.email").isEqualTo(dto.getNotifications().isEmail())
                    .jsonPath("$.notifications.sms").isEqualTo(dto.getNotifications().isSms())
                    .jsonPath("$.notifications.push").isEqualTo(dto.getNotifications().isPush())
                    .jsonPath("$.notifications.inApp").isEqualTo(dto.getNotifications().isInApp())
                    .jsonPath("$.preferences.dailyReport").isEqualTo(dto.getPreferences().isDailyReport())
                    .jsonPath("$.preferences.fraudAlerts").isEqualTo(dto.getPreferences().isFraudAlerts())
                    .jsonPath("$.preferences.maxTransactionAmt").isEqualTo(dto.getPreferences().getMaxTransactionAmt());
        }

        @Test
        @DisplayName("when request invalid, should return 400")
        void whenInvalidRequest_shouldReturn400() {
            UUID userId = UUID.randomUUID();
            // Missing required fields (e.g. null language) triggers validation failure
            UpdateUserPreferencesRequest invalid = new UpdateUserPreferencesRequest(
                    null, "", "", null, null
            );

            performPut(userId, invalid)
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("when service error, should return 500")
        void whenServiceError_shouldReturn500() {
            UUID userId = UUID.randomUUID();
            UpdateUserPreferencesRequest req = easyRandom.nextObject(UpdateUserPreferencesRequest.class);

            when(userService.updateUserPreferences(
                    eq(userId),
                    any(UpdateUserPreferencesRequest.class))
            )
                    .thenReturn(Mono.error(new RuntimeException("DB down")));

            performPut(userId, req)
                    .expectStatus().is5xxServerError()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Unexpected error: DB down")
                    .jsonPath("$.code").isEqualTo("INTERNAL_SERVER_ERROR");
        }
    }

}
