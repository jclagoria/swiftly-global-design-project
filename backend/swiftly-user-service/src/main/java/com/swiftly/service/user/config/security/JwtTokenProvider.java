package com.swiftly.service.user.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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

    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);
        return io.jsonwebtoken.Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                // you can add additional claims here if you want:
                // .claim("roles", rolesList)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Retrieves the subject (usually the username) from the token.
     * @param token the JWT token
     * @return the subject (username)
     */
    public String getSubject(String token) {
        // Parse the token and extract the subject (username)
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Validate the given token, returning true if valid, false if invalid.
     *
     * Note that invalid tokens will throw an exception, which is caught and
     * handled here.
     *
     * @param token the JWT token
     * @return true if the token is valid, false if it is not
     */
    public boolean validateToken(String token) {
        try {
            // Attempt to parse the token. If it is invalid, this will throw an exception.
            Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // If the token is invalid, return false
            return false;
        }
    }

    /**
     * If you want to examine claims later, you can parse the token and extract the claims.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     */
    public Claims parseClaims(String token) {
        // Parse the token and extract the claims
        return Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
