package com.swiftly.service.user.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain openEndpoints(ServerHttpSecurity http) {
        // build a composite matcher that fires on any of these paths:
        // NOTE: we use ServerWebExchangeMatchers to create a matcher that supports multiple paths
        //       in a way that is compatible with the reactive web framework.
        //       This allows us to specify paths that should be open without requiring authentication.
        var openPaths = ServerWebExchangeMatchers.matchers(
                // 1) Allow unauthenticated user registration:
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/api/v1/users/register"),

                // 2) Allow the raw OpenAPI JSON:
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/v3/api-docs/**"),

                // 3) Allow the Swagger-UI redirect + static assets:
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/swagger-ui.html"),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/swagger-ui/**"),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/webjars/**")

        );

        return http
                .securityMatcher(openPaths)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .build();
    }

}
