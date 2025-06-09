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
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Security configuration for WebFlux applications using JWT-based authentication.
 */
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

    /**
     * Returns an {@link AuthenticationWebFilter} that extracts the JWT from the Authorization header
     * and delegates authentication to the {@link JwtReactiveAuthenticationManager}.
     * <p>
     * If the JWT is invalid/expired, it returns a 401 response with a JSON body.
     * <p>
     * If the JWT is valid but revoked, it returns a 401 response with a JSON body.
     *
     * @param authManager the authentication manager to use
     * @return an {@link AuthenticationWebFilter} that handles JWT authentication
     */
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

    /**
     * This SecurityWebFilterChain is responsible for securing the endpoints of the API.
     * <p>
     * It does the following:
     * <ol>
     *     <li>Disables CSRF protection and CORS (we don't have a web UI, so we don't need these)</li>
     *     <li>Configures an authentication entry point for when authentication fails (e.g. invalid JWT)</li>
     *     <li>Authorizes the following endpoints to be public:
     *         <ul>
     *             <li>POST /register</li>
     *             <li>POST /login</li>
     *             <li>Swagger UI endpoints</li>
     *         </ul>
     *     </li>
     *     <li>Requires all other endpoints to be authenticated</li>
     *     <li>Inserts our JWT authentication filter at the AUTHENTICATION phase</li>
     *     <li>Disables Spring Security's default form-login, basic authentication, and logout endpoints</li>
     * </ol>
     *
     * @param http the ServerHttpSecurity to configure
     * @param jwtFilter the JWT authentication filter to insert
     * @return the configured SecurityWebFilterChain
     */
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

    /**
     * Handles authentication failures by setting the response status to 401 Unauthorized
     * and returning a JSON error message.
     *
     * @param exchange the ServerWebExchange containing the request and response
     * @param ex the exception that triggered the entry point
     * @return a Mono that completes when the response is written
     */
    private Mono<Void> handleAuthEntryPoint(ServerWebExchange exchange, AuthenticationException ex) {
        // Retrieve the 'WWW-Authenticate' header from the response
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
