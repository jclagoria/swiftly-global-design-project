package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.config.security.SecurityConfig;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
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
@DisplayName("UserController - DELETE /{userId}")
public class UserControllerDeleteUserTest extends AbstractUserControllerTest {

    @MockitoBean
    protected UserProfileResponseMapper userProfileResponseMapper;

    private String token;

    @BeforeEach
    void initAuth() {
        // same JWT/CSRF setup as in GET tests
        token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
    }

    private WebTestClient.ResponseSpec performDelete(UUID userId) {
        return webClient.delete()
                .uri(BASE + "/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("when success, should return 200 and valid body")
        void whenSuccess_shouldReturn200() {
            UUID userId = UUID.randomUUID();
            // service.deleteUser returns empty on success
            when(userService.deleteUser(eq(userId))).thenReturn(Mono.empty());

            performDelete(userId)
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("DELETE_OK")
                    .jsonPath("$.message").isEqualTo("User deleted successfully");
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
            when(userService.deleteUser(eq(userId))).thenReturn(Mono.error(exception));

            performDelete(userId)
                    .expectStatus().isEqualTo(expectedStatus)
                    .expectBody()
                    .jsonPath(jsonPath).isEqualTo(expectedMessage);
        }

        static Stream<Arguments> errorScenarios() {
            UUID missingId = UUID.randomUUID();
            UUID failId    = UUID.randomUUID();
            return Stream.of(
                    // not found → 404 with code USER_NOT_FOUND
                    Arguments.of(
                            missingId,
                            new UserNotFoundException(missingId),
                            404,
                            "$.code",
                            "USER_NOT_FOUND"
                    ),
                    // unexpected → 500 with error message
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
