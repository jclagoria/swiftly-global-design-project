package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.adapter.in.web.mapper.UserPreferencesResponseMapper;
import com.swiftly.service.user.adapter.in.web.mapper.UserProfileResponseMapper;
import com.swiftly.service.user.api.dto.*;
import com.swiftly.service.user.application.port.in.AuthService;
import com.swiftly.service.user.application.port.in.PreferencesService;
import com.swiftly.service.user.application.port.in.ProfileService;
import com.swiftly.service.user.application.port.in.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(
        name = "User Service",
        description = "Operations related to user registration, authentication, and profile management"
)
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final PreferencesService preferencesService;
    private final AuthService authService;
    private final ProfileService profileService;
    private final UserProfileResponseMapper userProfileResponseMapper;
    private final UserPreferencesResponseMapper userPreferencesResponseMapper;


    /**
     * Register a new user in the system.
     *
     * <p>On success, returns HTTP 201 with the created user’s data (excluding password).</p>
     *
     * @param request a JSON payload containing the new user's email, password, firstName, and lastName
     * @return a Mono that emits the created UserModel
     */
    @Operation(
            summary = "Register a new user",
            description = """
            Creates a new user account with the provided email, password, first name, and last name. 
            The password will be hashed using BCrypt before storage. 
            Returns the created user's information (including generated user ID and timestamps).
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON payload to register a new user",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterUserRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User successfully registered",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserCreationResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input (e.g., missing required fields or invalid email format)",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(
                                            example = """
                        {
                          "timestamp": "2025-06-02T15:20:30.000Z",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Validation failed for argument at index [0] in method: ...",
                          "path": "/v1/users/register"
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Email already in use",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(
                                            example = """
                        {
                          "code": "EMAIL_IN_USE",
                          "message": "Email already registered: user@example.com"
                        }
                        """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserCreationResponse> register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        return userManagementService.register(request)
                .map(createdUser -> new UserCreationResponse(
                        "User registered successfully",
                        createdUser.getId()
                ));
    }

    @Operation(
            summary = "Authenticate user and issue JWT",
            description = "Validates the provided email and password. On success, returns a JWT token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON payload to log a user in",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful; JWT returned",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LoginResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid email or password",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(
                                            example = """
                        {
                          "code": "INVALID_CREDENTIALS",
                          "message": "Invalid email or password"
                        }
                        """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Malformed request (e.g., missing fields)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    )
            }
    )
    @PostMapping("/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Logs out the user by invalidating the JWT token.
     *
     * @param authHeader the JWT token to invalidate
     * @return a Mono that completes when the logout is successful
     */
    @Operation(
            summary = "Logs out the user",
            description = "Logs out the user by invalidating the JWT token. The JWT token is expected to be passed in the Authorization header.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Logout successful"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid JWT token",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(
                                            example = """
                        {
                          "code": "INVALID_JWT",
                          "message": "Invalid JWT token"
                        }
                        """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        return authService.logout(token);
    }

    @Operation(
            summary     = "Retrieve a user’s profile by ID",
            description = "Returns user basic info + profile settings. Requires a valid JWT.",
            responses   = {
                    @ApiResponse(
                            responseCode = "200",
                            description  = "User profile found",
                            content      = @Content(schema = @Schema(implementation = UserProfileResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/{userId}")
    public Mono<UserProfileResponse> getProfile(@PathVariable UUID userId) {
        return profileService.getUserProfile(userId)
                .map(userProfileResponseMapper::toResponse);

    }

    @Operation(
            summary     = "Update user details",
            description = "Updates first/last name and profile settings (phone, address, locale, timezone).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to update",
                    required    = true,
                    content     = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = UpdateUserRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description  = "Updated profile returned",
                            content      = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema    = @Schema(implementation = UserProfileResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Validation failed"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<OperationResultResponse> updateUserDetails(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return profileService.updateUserProfile(userId, request)
                .thenReturn(OperationResultResponse
                        .builder()
                        .code("UPDATE_OK")
                        .message("User updated successfully").build());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<OperationResultResponse> deleteUser(@PathVariable UUID userId) {
        return userManagementService.deleteUser(userId)
                .thenReturn(
                        new OperationResultResponse("DELETE_OK",
                                "User deleted successfully")
                );
    }

    @GetMapping("/{userId}/preferences")
    public Mono<UserPreferenceResponse> getUserPreferences(@PathVariable UUID userId) {
        return preferencesService.getUserPreferences(userId)
                .map(userPreferencesResponseMapper::toResponse);
    }

    @Operation(
            summary     = "Update user preferences",
            description = "Creates or updates the user’s preferences document in MongoDB",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content  = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = UpdateUserPreferencesRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description  = "Updated preferences returned",
                            content      = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema      = @Schema(implementation = UserPreferenceResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Validation failed"),
                    @ApiResponse(responseCode = "404", description = "User not found")  // optional
            }
    )
    @PutMapping("/{userId}/preferences")
    public Mono<UserPreferenceResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserPreferencesRequest req
    ) {
        return preferencesService.updateUserPreferences(userId, req)
                .map(userPreferencesResponseMapper::toResponse);
    }

    @Operation(summary="Refresh JWT",
            description="Exchange a valid refresh token for a new access JWT")
    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public Mono<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest req
    ) {
        return authService.refreshToken(req);
    }

}
