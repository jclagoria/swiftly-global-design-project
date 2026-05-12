package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.UserPreferenceResponse;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.UserPreferencesNotFoundException;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UserController – GET /{userId}/preferences")
public class UserControllerGetPreferencesTest extends AbstractUserControllerTest{

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
    @DisplayName("getUserPreferences")
    class GetPreferencesTests {

        private String url(UUID userId) {
            return BASE + "/" + userId + "/preferences";
        }

        private WebTestClient.ResponseSpec performGet(UUID userId) {
            return webClient.get()
                    .uri(url(userId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange();
        }

        @Test
        @DisplayName("when success, should return 200 and valid body")
        void whenSuccess_shouldReturn200() {
            UUID userId = UUID.randomUUID();
            // generate a random domain model
            UserPreferencesModel model = TestFixtures
                    .randomPreferencesModel();
            // craft the expected response DTO
            UserPreferenceResponse dto = new UserPreferenceResponse(
                    model.getLanguage(),
                    model.getTimezone(),
                    model.getDefaultCurrency(),
                    null,
                    null
            );


            when(responseMapper.toResponse(eq(model)))
                    .thenReturn(dto);
            when(userService.getUserPreferences(eq(userId)))
                    .thenReturn(Mono.just(model));

            performGet(userId)
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.language").isEqualTo(dto.getLanguage())
                    .jsonPath("$.timezone").isEqualTo(dto.getTimezone())
                    .jsonPath("$.defaultCurrency").isEqualTo(dto.getDefaultCurrency());

        }

        @Test
        @DisplayName("when not found, should return 404")
        void whenNotFound_shouldReturn404() {
            UUID userId = UUID.randomUUID();
            when(userService.getUserPreferences(eq(userId)))
                    .thenReturn(Mono.error(new UserPreferencesNotFoundException(userId)));

            performGet(userId)
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("USER_PREFERENCES_NOT_FOUND")
                    .jsonPath("$.code")
                    .isEqualTo("User preferences not found for userId: " + userId);
        }

        @ParameterizedTest(name = "[{index}] service throws {1} → HTTP {2}")
        @MethodSource("errorScenarios")
        @DisplayName("error scenarios")
        void whenError_shouldReturnProperStatusAndBody(
                UUID userId,
                Throwable exception,
                int expectedStatus,
                String jsonPath,
                String expectedMessage
        ) {
            when(userService.getUserPreferences(eq(userId)))
                    .thenReturn(Mono.error(exception));

            performGet(userId)
                    .expectStatus().isEqualTo(expectedStatus)
                    .expectBody()
                    .jsonPath(jsonPath).isEqualTo(expectedMessage);
        }

        static Stream<Arguments> errorScenarios() {
            UUID badId = UUID.randomUUID();
            return Stream.of(
                    Arguments.of(
                            badId,
                            new RuntimeException("DB down"),
                            500,
                            "$.message",
                            "Unexpected error: DB down"
                    )
            );
        }
    }

}
