package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.api.dto.UpdateUserRequest;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.config.security.SecurityConfig;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest(controllers = UserController.class)
@Import(SecurityConfig.class)
public class AbstractUserControllerTest {

    protected static final String BASE = "/api/v1/users";

    @Autowired
    protected WebTestClient webClient;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected RevokedTokenRepository revokedTokenRepository;

    protected EasyRandom easyRandom;

    @BeforeEach
    void setUp() {
        webClient = webClient.mutateWith(csrf());
        easyRandom = new EasyRandom();
    }

    protected RegisterUserRequest sampleRegister() {
        // randomize all fields of your DTO
        return RegisterUserRequest.builder()
                .email(easyRandom.nextObject(String.class) + "@test.com")
                .password(easyRandom.nextObject(String.class))
                .firstName(easyRandom.nextObject(String.class))
                .lastName(easyRandom.nextObject(String.class))
                .build();
    }

    protected UpdateUserRequest sampleUpdate() {
        return UpdateUserRequest.builder()
                .firstName(easyRandom.nextObject(String.class))
                .lastName(easyRandom.nextObject(String.class))
                .phone(easyRandom.nextObject(String.class))
                .address(easyRandom.nextObject(String.class))
                .locale(easyRandom.nextObject(String.class))
                .timezone(easyRandom.nextObject(String.class))
                .build();
    }

    protected LoginRequest sampleLogin() {
        // randomize all fields of your DTO
        return LoginRequest.builder()
                .email(easyRandom.nextObject(String.class) + "@test.com")
                .password(easyRandom.nextObject(String.class))
                .build();
    }

}
