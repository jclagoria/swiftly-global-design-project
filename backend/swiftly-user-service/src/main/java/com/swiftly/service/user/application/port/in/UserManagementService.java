package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.domain.model.UserModel;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserManagementService {

    /**
     * Registers a new user based on the provided registration request.
     *
     * @param request the registration request containing user details
     * @return a Mono emitting the registered UserModel
     */
    Mono<UserModel> register(RegisterUserRequest request);

    /**
     * Deletes an existing user from the system.
     *
     * @param userId the UUID of the user to be deleted
     * @return a Mono emitting a void value, indicating the deletion was successful
     */
    Mono<Void> deleteUser(UUID userId);

}
