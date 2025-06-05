package com.swiftly.service.user.adapter.in.web.controller;

import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.LoginResponse;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.api.dto.UserCreationResponse;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.domain.model.UserModel;
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

@Tag(
        name = "User Service",
        description = "Operations related to user registration, authentication, and profile management"
)
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@AllArgsConstructor
public class UserController {

    private final UserService userService;

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
        return userService.register(request)
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
        return userService.login(request)
                .map(LoginResponse::new);
    }
}
