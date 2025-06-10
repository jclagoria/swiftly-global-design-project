package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.api.dto.UpdateUserRequest;
import com.swiftly.service.user.domain.model.UserModel;
import com.swiftly.service.user.domain.model.UserProfileModel;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service interface for user-related operations.
 *
 * This interface defines the contract for registering, logging in,
 * and logging out users within the application layer.
 */
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

    /**
     * Retrieves a user profile based on the provided user ID.
     *
     * @param userId the UUID of the user whose profile is to be retrieved
     * @return a Mono emitting the UserProfileModel associated with the given user ID
     */
    Mono<UserProfileModel> getUserProfile(UUID userId);

    /**
     * Updates an existing user's profile based on the provided request.
     *
     * @param userId        the UUID of the user whose profile is to be updated
     * @param updateUserRequest the request containing the new user profile details
     * @return a Mono emitting a void value, indicating the update was successful
     */
    Mono<Void> updateUserProfile(UUID userId, UpdateUserRequest updateUserRequest);

    Mono<Void> deleteUser(UUID userId);
}
