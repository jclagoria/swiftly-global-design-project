package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.api.dto.RefreshTokenRequest;
import com.swiftly.service.user.api.dto.RefreshTokenResponse;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * Logs in an existing user based on the provided login request.
     *
     * @param request the login request containing user credentials
     * @return a Mono emitting the UserModel of the successfully logged-in user
     */
    Mono<LoginResponse> login(LoginRequest request);

    /**
     * Logs out an existing user based on the provided JWT token.
     *
     * @param token the JWT token to log out
     * @return a Mono emitting a void value, indicating the logout was successful
     */
    Mono<Void> logout(String token);


    /**
     * Exchanges a valid refresh token for a new access JWT.
     *
     * @param request a RefreshTokenRequest containing the refresh token
     * @return a Mono emitting a RefreshTokenResponse containing the new JWT token
     */
    Mono<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);

}
