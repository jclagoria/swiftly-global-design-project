package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.UserProfileResponse;
import com.swiftly.service.user.config.security.SecurityConfig;
import com.swiftly.service.user.data.TestFixtures;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import com.swiftly.service.user.domain.model.UserProfileModel;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController - GET /{userId}")
public class UserControllerGetProfileTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserProfileResponseMapper responseMapper;

    private String token;

    @BeforeEach
    void initAuth() {
        token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
        // No logout call here since this is a GET profile
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        private String url(UUID userId) {
            return BASE + "/" + userId;
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
            UserProfileModel model = TestFixtures.randomProfileModel();
            UserProfileResponse response = new UserProfileResponse(
                    model.getId(), model.getEmail(), model.getFirstName(), model.getLastName(),
                    model.getCreatedAt(), model.getPhone(), model.getAddress(), model.getLocale(),
                    model.getTimezone(), model.getUpdatedAt());

            when(userService.getUserProfile(eq(userId))).thenReturn(Mono.just(model));
            when(responseMapper.toResponse(eq(model))).thenReturn(response);

            performGet(userId)
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.userId").isEqualTo(response.getUserId().toString())
                    .jsonPath("$.email").isEqualTo(response.getEmail())
                    .jsonPath("$.firstName").isEqualTo(response.getFirstName())
                    .jsonPath("$.lastName").isEqualTo(response.getLastName())
                    .jsonPath("$.phone").isEqualTo(response.getPhone())
                    .jsonPath("$.address").isEqualTo(response.getAddress())
                    .jsonPath("$.locale").isEqualTo(response.getLocale())
                    .jsonPath("$.timezone").isEqualTo(response.getTimezone());
        }

        @ParameterizedTest(name = "[{index}] when service throws {1}, expect {2}")
        @MethodSource("errorScenarios")
        @DisplayName("error scenarios")
        void whenError_shouldReturnProperStatusAndBody(
                UUID userId,
                Throwable exception,
                int expectedStatus,
                String jsonPath,
                String expectedMessage
        ) {
            when(userService.getUserProfile(eq(userId))).thenReturn(Mono.error(exception));

            performGet(userId)
                    .expectStatus().isEqualTo(expectedStatus)
                    .expectBody()
                    .jsonPath(jsonPath).isEqualTo(expectedMessage);
        }

        static Stream<Arguments> errorScenarios() {
            UUID missingId = UUID.randomUUID();
            UUID failId = UUID.randomUUID();
            return Stream.of(
                    Arguments.of(
                            missingId,
                            new UserNotFoundException(missingId),
                            404,
                            "$.message",
                            "USER_NOT_FOUND",
                            "$.code",
                            "User not found with ID: " + missingId
                    ),
                    Arguments.of(
                            failId,
                            new RuntimeException("DB fail"),
                            500,
                            "$.message",
                            "Unexpected error: DB fail"
                    )
            );
        }
    }

}

