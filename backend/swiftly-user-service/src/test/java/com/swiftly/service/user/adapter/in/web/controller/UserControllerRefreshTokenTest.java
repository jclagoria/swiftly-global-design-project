package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.RefreshTokenRequest;
import com.swiftly.service.user.api.dto.RefreshTokenResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UserController - POST /refresh-token")
public class UserControllerRefreshTokenTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserProfileResponseMapper responseMapper;

    @MockitoBean
    protected UserPreferencesResponseMapper userPreferencesResponseMapper;

    private String token;

    @BeforeEach
    void initAuth() {
        token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        private String url() {
            return BASE + "/refresh-token";
        }

        private WebTestClient.ResponseSpec performPost(RefreshTokenRequest req) {
            return webClient.post()
                    .uri(url())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .exchange();
        }

        @Test
        @DisplayName("when success, should return 200 and valid body")
        void whenSuccess_shouldReturn200() {
            RefreshTokenRequest req = sampleRefreshRequest();
            RefreshTokenResponse expected = RefreshTokenResponse.builder()
                    .token("new-access-token")
                    .build();

            when(userService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenReturn(Mono.just(expected));

            performPost(req)
                    .expectStatus().isOk()
                    .expectBody(RefreshTokenResponse.class)
                    .value(resp -> assertEquals(expected.getToken(), resp.getToken()));
        }

        @Test
        @DisplayName("when validation fails (missing required fields), should return 400")
        void whenValidationFails_shouldReturn400() {
            RefreshTokenRequest req = RefreshTokenRequest.builder().build();

            performPost(req)
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.error").isEqualTo("Bad Request");
        }

        @ParameterizedTest(name = "[{index}] error: {0}")
        @MethodSource("errorScenarios")
        @DisplayName("when service error, should map to proper status and body")
        void whenError_shouldReturnProperStatusAndBody(
                Throwable exception,
                int expectedStatus,
                String jsonPath,
                String expectedValue
        ) {
            RefreshTokenRequest req = sampleRefreshRequest();
            when(userService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenReturn(Mono.error(exception));

            performPost(req)
                    .expectStatus().isEqualTo(expectedStatus)
                    .expectBody()
                    .jsonPath(jsonPath).isEqualTo(expectedValue);
        }

        static Stream<Arguments> errorScenarios() {
            return Stream.of(
                    Arguments.of(
                            new RuntimeException("DB fail"),
                            500,
                            "$.message",
                            "Unexpected error: DB fail"
                    )
            );
        }

        private RefreshTokenRequest sampleRefreshRequest() {
            return RefreshTokenRequest.builder()
                    .refreshToken(easyRandom.nextObject(String.class))
                    .build();
        }
    }

}
