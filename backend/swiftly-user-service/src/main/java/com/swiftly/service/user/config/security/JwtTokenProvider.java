package com.swiftly.service.user.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static io.jsonwebtoken.Jwts.*;

/**
 * Utility for creating JWT tokens.
 * We use HS256 (HMAC + SHA-256) behind the scenes.
 *
 * You can inject this bean anywhere you need to issue a token
 * (e.g. after validating user credentials during login).
 */
@Component
public class JwtTokenProvider {

    /**
     * The secret key used to sign the JWT tokens.
     * This should be kept secret and secure.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-seconds}")
    private long jwtExpiration;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a JWT token containing the given subject (usually the username).
     *
     * @param subject the subject (username) to include in the JWT token
     * @return the generated JWT token
     */
    public String createToken(String subject) {
        // The current instant
        Instant now = Instant.now();

        // The expiration instant
        Instant expirationInstant = now.plusSeconds(jwtExpiration);

        return builder()
                // The subject of the token
                .subject(subject)
                // The time the token was issued
                .issuedAt(Date.from(now))
                // The time the token will expire
                .expiration(Date.from(expirationInstant))
                // You can add additional claims here if you want, e.g. roles
                // .claim("roles", rolesList)
                // .claim("permissions", permissionsList)
                // Sign the token using the secret key
                .signWith(signingKey, SIG.HS256)
                // Compact the token to a string
                .compact();
    }

    /**
     * Retrieves the subject (usually the username) from the token.
     *
     * @param token the JWT token
     * @return the subject (username)
     */
    public String getSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Validates the given token, returning true if valid, false otherwise.
     *
     * @param token the JWT token
     * @return true if the token is valid, false if it is not
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);  // throws if invalid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses and returns claims from the JWT token.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     */
    public Claims parseClaims(String token) {
        return parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token).getPayload();
    }
}
