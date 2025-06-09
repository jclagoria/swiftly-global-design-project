package com.swiftly.service.user.config.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom WebFilter that extracts the JWT from the Authorization header
 * and delegates authentication to the JwtReactiveAuthenticationManager.
 */
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {

    public JwtAuthenticationWebFilter(JwtReactiveAuthenticationManager jwtAuthenticationManager) {
        super(jwtAuthenticationManager);
        // STILL: convert the “Authorization: Bearer <…>” header into a Spring Authentication
        setServerAuthenticationConverter(this::extractBearerToken);
    }

    /**
     * Extracts the JWT from the Authorization header.
     * <p>
     * If the header is present and starts with "Bearer ", it extracts the JWT token
     * and returns a {@link BearerTokenAuthenticationToken} containing the token.
     * <p>
     * If the header is missing or invalid, it returns an empty Mono, which triggers
     * the entry point/failure handler.
     *
     * @param serverWebExchange the ServerWebExchange containing the request
     * @return a Mono emitting an {@link Authentication} object if the Authorization header is valid, or an empty Mono if it is not
     */
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
