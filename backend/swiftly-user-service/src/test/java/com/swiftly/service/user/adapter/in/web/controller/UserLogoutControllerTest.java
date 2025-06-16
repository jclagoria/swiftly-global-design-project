package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UserController - Logout /logout")
public class UserLogoutControllerTest extends AbstractUserControllerTest {

    @MockitoBean
    private UserProfileResponseMapper responseMapper;

    @MockitoBean
    protected UserPreferencesResponseMapper userPreferencesResponseMapper;

    @Test
    void whenSuccess_shouldReturn204() {
        String token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
        when(authService.logout(token)).thenReturn(Mono.empty());

        webClient.post()
                .uri(BASE + "/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
    }

    @ParameterizedTest(name = "Header={0}")
    @CsvSource({
            "'', 401, $.error, Missing Authorization header or not a Bearer token",
            "'Basic foo', 401, $.error, Missing Authorization header or not a Bearer token"
    })
    void whenBadHeader_shouldReturn401(
            String headerValue,
            int status,
            String path,
            String expectedMsg
    ) {
        var request = webClient.post().uri(BASE + "/logout");
        if (!headerValue.isEmpty()) {
            request = request.header(HttpHeaders.AUTHORIZATION, headerValue);
        }

        request.exchange()
                .expectStatus().isEqualTo(HttpStatus.valueOf(status))
                .expectBody()
                .jsonPath(path).isEqualTo(expectedMsg);
    }

    @Test
    void whenServiceError_shouldReturn500() {
        String token = UUID.randomUUID().toString();
        when(jwtTokenProvider.parseClaims(token)).thenReturn(mock(Claims.class));
        when(revokedTokenRepository.existsByToken(token)).thenReturn(Mono.just(false));
        when(authService.logout(token))
                .thenReturn(Mono.error(new RuntimeException("DB fail")));

        webClient.post()
                .uri(BASE + "/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Unexpected error: DB fail");
    }

}
