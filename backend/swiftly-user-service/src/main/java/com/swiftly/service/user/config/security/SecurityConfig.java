package com.swiftly.service.user.config.security;

import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final RevokedTokenRepository revokedTokenRepository;

    @Bean
    public ReactiveAuthenticationManager jwtAuthenticationManager() {
        return new JwtReactiveAuthenticationManager(tokenProvider, revokedTokenRepository);
    }

    @Bean
    public AuthenticationWebFilter jwtAuthenticationWebFilter(ReactiveAuthenticationManager authManager) {
        JwtAuthenticationWebFilter filter =
                new JwtAuthenticationWebFilter((JwtReactiveAuthenticationManager) authManager);

        // (Optional) If you also want a custom failure handler when a token is supplied but is invalid/expired:
        filter.setAuthenticationFailureHandler((exchange, exception) -> {
            // e.g. malformed or expired JWT → return 401 with a JSON body
            exchange.getExchange().getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getExchange().getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"error\":\"Token invalid, expired, or revoked\"}";
            DataBuffer buffer = exchange.getExchange().getResponse()
                    .bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getExchange().getResponse().writeWith(Mono.just(buffer));
        });

        // Keep it stateless (no SecurityContext in session)
        filter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return filter;
    }

    @Bean
    public SecurityWebFilterChain openEndpoints(ServerHttpSecurity http,
                                                AuthenticationWebFilter jwtFilter) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleAuthEntryPoint)
                )
                .authorizeExchange(ex -> ex
                        // 1) Public: /register & /login
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/users/register",
                                "/api/v1/users/login").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html",
                                "/swagger-ui/**", "/webjars/**").permitAll()

                        // 2) All other endpoints (including /logout) must be authenticated
                        .anyExchange().authenticated()
                )
                // ──▶ 3) Insert our JWT filter at the AUTHENTICATION phase:
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 4) Disable Spring’s defaults (we only want JWT, no form‐login/basic/logout)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable);

        return http.build();
    }

    private Mono<Void> handleAuthEntryPoint(ServerWebExchange exchange, AuthenticationException ex) {
        String authhander = exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body;

        if (authhander == null || !authhander.startsWith("Bearer ")) {
            body = "{\"error\":\"Missing Authorization header or not a Bearer token\"}";
        } else {
            body = "{\"error\":\"Invalid, expired, or revoked token\"}";
        }

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
