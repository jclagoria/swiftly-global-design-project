package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.UpdateUserRequest;
import com.swiftly.service.user.domain.model.UserProfileModel;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProfileService {

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

}
