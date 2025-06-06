package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.domain.model.UserModel;
import reactor.core.publisher.Mono;

public interface UserService {
    /**
     * Registers a new user based on the provided registration request.
     *
     * @param request the registration request containing user details
     * @return a Mono emitting the registered UserModel
     */
    Mono<UserModel> register(RegisterUserRequest request);

    /**
     * Logs in an existing user based on the provided login request.
     *
     * @param request the login request containing user credentials
     * @return a Mono emitting the UserModel of the successfully logged-in user
     */
    Mono<String> login(LoginRequest request);

    /**
     * Logs out an existing user based on the provided JWT token.
     *
     * @param token the JWT token to log out
     * @return a Mono emitting a void value, indicating the logout was successful
     */
    Mono<Void> logout(String token);
}
