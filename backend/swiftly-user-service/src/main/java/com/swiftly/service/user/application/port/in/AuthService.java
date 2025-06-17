package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.api.dto.RefreshTokenRequest;
import com.swiftly.service.user.api.dto.RefreshTokenResponse;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * Authenticate user credentials and issue both an access JWT and a refresh token.
     *
     * @param request contains user email and password
     * @return Mono emitting a {@link LoginResponse} with JWT and refresh token
     * @throws InvalidCredentialsException if email not found or password mismatch
     */
    Mono<LoginResponse> login(LoginRequest request);

    /**
     * Revoke an access JWT by persisting it in the revoked_tokens table.
     * Parses the token off the blocking thread to avoid event-loop stalls.
     *
     * @param token the JWT to revoke
     * @return Mono that completes when the token is persisted
     * @throws InvalidCredentialsException if the token is malformed or cannot be parsed
     */
    Mono<Void> logout(String token);


    /**
     * Consume a refresh token, revoke it, and issue a fresh pair of tokens.
     * Entire revoke-and-reissue sequence is wrapped in a reactive transaction.
     *
     * @param request contains the refresh token string
     * @return Mono emitting a {@link RefreshTokenResponse} with new JWT and refresh token
     * @throws InvalidCredentialsException if token malformed, not found, expired, or already revoked
     */
    Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);

}
