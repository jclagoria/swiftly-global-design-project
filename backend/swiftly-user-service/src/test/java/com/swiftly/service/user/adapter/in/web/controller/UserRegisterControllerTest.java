package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.UserCreationResponse;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.model.UserModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = UserController.class)
@DisplayName("UserController - Register /register")
public class UserRegisterControllerTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserProfileResponseMapper responseMapper;
    @Test
    void whenSuccess_shouldReturn201AndUserId() {
        var req = sampleRegister();
        var model = UserModel.builder()
                .id(UUID.randomUUID())
                .email(req.getEmail())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .build();

        when(userService.register(any())).thenReturn(Mono.just(model));

        webClient.post().uri(BASE + "/register")
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserCreationResponse.class)
                .value(resp -> {
                    assert resp.getMessage().equals("User registered successfully");
                    assert resp.getUserId().equals(model.getId());
                });
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("errorScenarios")
    void whenError_shouldReturnMappedStatusAndMessage(
            Exception exception,
            HttpStatus status,
            String messagePath,
            String expectedMessage
    ) {
        when(userService.register(any())).thenReturn(Mono.error(exception));

        webClient.post().uri(BASE + "/register")
                .bodyValue(sampleRegister())
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody()
                .jsonPath(messagePath).isEqualTo(expectedMessage);
    }

    /**
     * Provides test cases for error scenarios during user registration.
     * Each test case is represented as an argument consisting of:
     * - The exception to be thrown
     * - The expected HTTP status
     * - The JSON path to the error message
     * - The expected error message
     *
     * @return a stream of arguments for parameterized tests
     */
    static Stream<Arguments> errorScenarios() {
        String email = "foo@test.com";
        return Stream.of(
                // Test case for EmailAlreadyInUseException
                Arguments.of(
                        new EmailAlreadyInUseException(email),
                        HttpStatus.CONFLICT,
                        "$.message",
                        "Email already register: " + email
                ),
                // Test case for unexpected runtime exception
                Arguments.of(
                        new RuntimeException("DB fail"),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "$.message",
                        "Unexpected error: DB fail"
                )
        );
    }
}
