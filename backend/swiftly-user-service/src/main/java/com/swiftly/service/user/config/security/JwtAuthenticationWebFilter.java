package com.swiftly.service.user.config.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {

    public JwtAuthenticationWebFilter(JwtReactiveAuthenticationManager jwtAuthenticationManager) {
        super(jwtAuthenticationManager);
        // STILL: convert the “Authorization: Bearer <…>” header into a Spring Authentication
        setServerAuthenticationConverter(this::extractBearerToken);
    }

    private Mono<Authentication> extractBearerToken(ServerWebExchange serverWebExchange) {
        String auth = serverWebExchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            return Mono.just(new BearerTokenAuthenticationToken(token));
        }

        // If there is no Bearer header, return Mono.empty() → triggers the entry‐point/failure handler
        return Mono.empty();
    }

}
