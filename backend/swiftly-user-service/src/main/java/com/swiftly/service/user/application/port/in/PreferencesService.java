package com.swiftly.service.user.application.port.in;

import com.swiftly.service.user.api.dto.UpdateUserPreferencesRequest;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application service interface for user-related operations.
 *
 * This interface defines the contract for registering, logging in,
 * and logging out users within the application layer.
 */
public interface PreferencesService {

    /**
     * Retrieves a user's preferences based on the provided user ID.
     *
     * @param userId the UUID of the user whose preferences are to be retrieved
     * @return a Mono emitting the UserPreferencesModel associated with the given user ID
     */
    Mono<UserPreferencesModel> getUserPreferences(UUID userId);

    /**
     * Updates an existing user's preferences based on the provided request.
     *
     * @param userId                the UUID of the user whose preferences are to be updated
     * @param updateUserPreferencesRequest the request containing the new user preferences
     * @return a Mono emitting the UserPreferencesModel associated with the given user ID
     *         after the update operation
     */
    Mono<UserPreferencesModel> updateUserPreferences(UUID userId,
                                                     UpdateUserPreferencesRequest updateUserPreferencesRequest);

}
