package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserLoginControllerTest extends AbstractUserControllerTest {

    @Test
    void whenSuccess_shouldReturn200AndToken() {
        var req = sampleLogin();
        String token = "JWT-TOKEN";
        when(userService.login(any())).thenReturn(Mono.just(token));

        webClient.post().uri(BASE + "/login")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .value(resp -> {
                    assert resp.getToken().equals(token);
                });
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("loginErrorScenarios")
    void whenError_shouldReturnMappedStatusAndMessage(
            Exception exception,
            HttpStatus status,
            String messagePath,
            String expectedMessage
    ) {
        when(userService.login(any())).thenReturn(Mono.error(exception));

        webClient.post().uri(BASE + "/login")
                .bodyValue(sampleLogin())
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody()
                .jsonPath(messagePath).isEqualTo(expectedMessage);
    }

    /**
     * Scenarios for testing how the UserLoginController behaves when an error occurs while logging in.
     * <p>
     * This method returns a Stream of Arguments, where each Argument represents one scenario.
     * The arguments are:
     * <ol>
     *     <li>The exception that should be thrown by the UserService when attempting to login</li>
     *     <li>The HTTP status that the UserLoginController should return</li>
     *     <li>The JSON path to the error message that the UserLoginController should return</li>
     *     <li>The expected error message that the UserLoginController should return</li>
     * </ol>
     * @return a Stream of Arguments that define the scenarios for testing login errors
     */
    static Stream<Arguments> loginErrorScenarios() {
        return Stream.of(
                Arguments.of(
                        // Thrown when the user provides invalid credentials
                        new InvalidCredentialsException(),
                        // Return a 401 Unauthorized response
                        HttpStatus.UNAUTHORIZED,
                        // The error message is in the $.message field
                        "$.message",
                        // The error message should be "INVALID_CREDENTIALS"
                        "INVALID_CREDENTIALS"
                ),
                Arguments.of(
                        // Thrown when something unexpected happens
                        new RuntimeException("oops"),
                        // Return a 500 Internal Server Error response
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        // The error message is in the $.message field
                        "$.message",
                        // The error message should be "Unexpected error: oops"
                        "Unexpected error: oops"
                )
        );
    }
}
