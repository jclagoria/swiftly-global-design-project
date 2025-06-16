package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("UserController - Login /login")
public class UserLoginControllerTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserProfileResponseMapper responseMapper;

    @MockitoBean
    private UserPreferencesResponseMapper preferencesMapper;

    @Test
    @DisplayName("whenSuccess_shouldReturn200AndToken")
    void whenSuccess_shouldReturn200AndToken() {
        var req = sampleLogin();
        LoginResponse loginResponse = easyRandom.nextObject(LoginResponse.class);

        when(authService.login(any())).thenReturn(Mono.just(loginResponse));

        webClient.post().uri(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .value(resp -> {
                    assert resp.getToken().equals(loginResponse.getToken());
                    assert resp.getRefreshToken().equals(loginResponse.getRefreshToken());
                });
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("loginErrorScenarios")
    @DisplayName("whenError_shouldReturnMappedStatusAndMessage")
    void whenError_shouldReturnMappedStatusAndMessage(
            Exception exception,
            HttpStatus status,
            String messagePath,
            String expectedMessage
    ) {
        when(authService.login(any())).thenReturn(Mono.error(exception));

        webClient.post().uri(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sampleLogin())
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody()
                .jsonPath(messagePath).isEqualTo(expectedMessage);
    }

    /**
     * New validation test: empty payload → 400 Bad Request
     */
    @Test
    @DisplayName("whenPayloadEmpty_shouldReturn400")
    void whenPayloadEmpty_shouldReturn400() {
        Map<String, Object> empty = Collections.emptyMap();

        webClient.post().uri(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(empty)
                .exchange()
                .expectStatus().isBadRequest();
    }

    static Stream<Arguments> loginErrorScenarios() {
        return Stream.of(
                Arguments.of(
                        new InvalidCredentialsException(),
                        HttpStatus.UNAUTHORIZED,
                        "$.message",
                        "INVALID_CREDENTIALS"
                ),
                Arguments.of(
                        new RuntimeException("oops"),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "$.message",
                        "Unexpected error: oops"
                )
        );
    }
}
