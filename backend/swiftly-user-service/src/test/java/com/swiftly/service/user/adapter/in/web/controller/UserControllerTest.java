package com.swiftly.service.user.adapter.in.web.controller;

import com.jayway.jsonpath.InvalidJsonException;
import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.api.dto.UserCreationResponse;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.config.security.JwtAuthenticationWebFilter;
import com.swiftly.service.user.config.security.JwtReactiveAuthenticationManager;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.config.security.SecurityConfig;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import com.swiftly.service.user.domain.model.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

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
    void register_whenSuccess_returnsCreatedUserId() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(Mono.just(expectedModel));

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(UserCreationResponse.class)
                .value(response -> {
                    assertThat(response.getMessage()).isEqualTo("User registered successfully");
                    assertThat(response.getUserId()).isEqualTo(expectedModel.getId());
                });
    }

    @Test
    void register_whenServiceErrors_forwardsError() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(Mono.error(new EmailAlreadyInUseException(request.getEmail())));

        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Email already register: " + request.getEmail());
    }

    @Test
    void register_whenServiceFails_returnsInternalServerError() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("DB failure")));

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").value(msg ->
                        assertThat(msg).isEqualTo("Unexpected error: DB failure"));
    }

    @Test
    void login_whenSuccess_returnsToken() {
        // Given
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);
        String expectedToken = "JWT_TOKEN";

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(expectedToken));

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(LoginResponse.class)
                .value(response -> {
                    assert response.getToken().equals(expectedToken);
                });
    }

    @Test
    void login_whenInvalidCredentials_returnsUnauthorized() {
        // Given
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException()));

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void login_whenUnexpectedError_returnsInternalServerError() {
        // Given
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected")));

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Unexpected error: Unexpected");
    }

    @Test
    void logout_whenSuccess_returnsNoContent() {
        // Given
        String token = easyRandom.nextObject(UUID.class).toString();
        Claims dummyClaims = mock(Claims.class);

        when(jwtTokenProvider.parseClaims(token)).thenReturn(dummyClaims);
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
        when(userService.logout(token)).thenReturn(Mono.empty());

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/logout")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void logout_whenMissingAuthorizationHeader_returnsUnauthorized() {
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/logout")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error")
                    .isEqualTo("Missing Authorization header or not a Bearer token");
    }

    @Test
    void logout_whenInvalidAuthorizationHeader_returnsUnauthorized() {
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/logout")
                .header("Authorization", "InvalidHeader")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Missing Authorization header or not a Bearer token");
    }

    @Test
    void logout_whenServiceFails_returnsInternalServerError() {
        String token = easyRandom.nextObject(UUID.class).toString();
        Claims dummyClaims = mock(Claims.class);

        when(jwtTokenProvider.parseClaims(token))
                .thenReturn(dummyClaims);
        when(revokedTokenRepository.existsByToken(token))
                .thenReturn(Mono.just(false));
        when(userService.logout(token))
                .thenReturn(Mono.error(new RuntimeException("DB failure")));

        // When / Then
        webClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/logout")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Unexpected error: DB failure");
    }

}