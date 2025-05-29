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
        var openPaths = ServerWebExchangeMatchers.matchers(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST,   "/api/v1/users/register"),
                ServerWebExchangeMatchers.pathMatchers("/v3/api-docs/**"),
                ServerWebExchangeMatchers.pathMatchers("/swagger-ui/**"),
                ServerWebExchangeMatchers.pathMatchers("/swagger-ui.html")
        );

        return http
                .securityMatcher(openPaths)               // use only when one of those paths matches
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                // disable any other auth filters so JWT isn’t applied here:
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .build();
    }

}
