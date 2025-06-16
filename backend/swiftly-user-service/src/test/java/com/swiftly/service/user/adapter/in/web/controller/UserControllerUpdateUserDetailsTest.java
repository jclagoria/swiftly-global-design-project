package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.OperationResultResponse;
import com.swiftly.service.user.api.dto.UpdateUserRequest;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UserController - PUT /{userId}")
public class UserControllerUpdateUserDetailsTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserProfileResponseMapper responseMapper;

    @MockitoBean
    protected UserPreferencesResponseMapper userPreferencesResponseMapper;// ← mock the mapper

    private String token;

    @BeforeEach
    void initAuth() {
        token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
    }

    @Nested
    @DisplayName("updateUserDetails")
    class UpdateUserDetailsTests {

        private String url(UUID userId) {
            return BASE + "/" + userId;
        }

        private WebTestClient.ResponseSpec performPut(UUID userId, UpdateUserRequest req) {
            return webClient.put()
                    .uri(url(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(req)
                    .exchange();
        }

        @Test
        @DisplayName("when success, should return 200 and valid body")
        void whenSuccess_shouldReturn200() {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest req = sampleUpdate();

            when(profileService.updateUserProfile(
                        eq(userId),
                        any(UpdateUserRequest.class)
                )).thenReturn(Mono.empty());

            performPut(userId, req)
                    .expectStatus().isOk()
                    .expectBody(OperationResultResponse.class)
                    .value(resp -> {
                        assertEquals("UPDATE_OK", resp.getCode());
                        assertEquals("User updated successfully", resp.getMessage());
                    });
        }

        @Test
        @DisplayName("when validation fails (missing required fields), should return 400")
        void whenValidationFails_shouldReturn400() {
            UUID userId = UUID.randomUUID();
            UpdateUserRequest req = UpdateUserRequest.builder().build();

            performPut(userId, req)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.error").isEqualTo("Bad Request");
        }

        @ParameterizedTest(name = "[{index}] error: {1}")
        @MethodSource("errorScenarios")
        @DisplayName("when service error, should map to proper status and body")
        void whenError_shouldReturnProperStatusAndBody(
                UUID userId,
                Throwable exception,
                int expectedStatus,
                String jsonPath,
                String expectedValue
        ) {
            when(profileService.updateUserProfile(eq(userId), any(UpdateUserRequest.class)))
                    .thenReturn(Mono.error(exception));

            UpdateUserRequest req = UpdateUserRequest.builder()
                    .firstName("X").lastName("Y").build();

            performPut(userId, req)
                    .expectStatus().isEqualTo(expectedStatus)
                    .expectBody()
                    .jsonPath(jsonPath).isEqualTo(expectedValue);
        }

        static Stream<Arguments> errorScenarios() {
            UUID missingId = UUID.randomUUID();
            UUID failId    = UUID.randomUUID();
            return Stream.of(
                    Arguments.of(
                            missingId,
                            new UserNotFoundException(missingId),
                            404,
                            "$.message",
                            "USER_NOT_FOUND"
                    ),
                    Arguments.of(
                            missingId,
                            new UserNotFoundException(missingId),
                            404,
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
