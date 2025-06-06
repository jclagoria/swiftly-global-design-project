package com.swiftly.service.user.config.security;

import com.swiftly.service.user.adapter.out.persistence.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Reactive authentication manager that authenticates JWTs
 * and checks if they have been revoked.
 */
@RequiredArgsConstructor
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtTokenProvider jwtTokenProvider;
    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Authenticates the given authentication object, which should contain a Bearer token.
     *
     * @param authentication the authentication object containing the Bearer token
     * @return a Mono emitting the authenticated token or an error if authentication fails
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        // Check if the authentication object is a BearerTokenAuthenticationToken
        if (!(authentication instanceof BearerTokenAuthenticationToken)) {
            return Mono.error(
                    new BadCredentialsException("No Bearer token in request")
            );
        }

        // Extract the token from the authentication object
        String token = ((BearerTokenAuthenticationToken) authentication).getToken();

        Claims claims;
        try {
            // Parse the claims from the JWT token
            claims = jwtTokenProvider.parseClaims(token);
        } catch (JwtException ex) {
            // Return an error if the JWT is invalid or expired
            return Mono.error(
                    new BadCredentialsException("JWT invalid or expired: " + ex.getMessage())
            );
        }

        // Check if the token has been revoked
        return revokedTokenRepository.existsByToken(token)
                .flatMap(isRevoked -> {
                    if (Boolean.TRUE.equals(isRevoked)) {
                        // Return an error if the token is revoked
                        return Mono.error(
                                new BadCredentialsException("JWT has been revoked")
                        );
                    }

                    // Retrieve the user ID (subject) from the claims
                    String userId = claims.getSubject();
                    // Create an authentication token with no authorities
                    AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                    );
                    // Return the authenticated token
                    return Mono.just(auth);
                });
    }
}
