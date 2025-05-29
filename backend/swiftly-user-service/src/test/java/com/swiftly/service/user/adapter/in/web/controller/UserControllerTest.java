package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.config.security.SecurityConfig;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.model.UserModel;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private UserService userService;

    private EasyRandom easyRandom;
    private RegisterUserRequest request;
    private UserModel expectedModel;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();

        // Create a random registration request
        request = RegisterUserRequest.builder()
                .email(easyRandom.nextObject(String.class) + "@test.com")
                .password(easyRandom.nextObject(String.class))
                .firstName(easyRandom.nextObject(String.class))
                .lastName(easyRandom.nextObject(String.class))
                .build();

        // Expected model returned by service
        expectedModel = UserModel.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash("ENCODED")
                .build();
    }

    @Test
    void register_whenSuccess_returnsCreatedAndBody() {
        // stub service to return expectedModel
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(Mono.just(expectedModel));

        webClient.mutateWith(csrf())
                .post()
                .uri("/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(UserModel.class)
                .value(model -> {
                    assert model.getEmail().equals(expectedModel.getEmail());
                    assert model.getFirstName().equals(expectedModel.getFirstName());
                    assert model.getLastName().equals(expectedModel.getLastName());
                });
    }

    @Test
    void register_whenServiceErrors_forwardsError() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(Mono.error(new EmailAlreadyInUseException(request.getEmail())));

        webClient.mutateWith(csrf())
                .post()
                .uri("/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Email already register: " + request.getEmail());
    }

}