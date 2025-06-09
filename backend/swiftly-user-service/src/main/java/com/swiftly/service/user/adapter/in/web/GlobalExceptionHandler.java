package com.swiftly.service.user.adapter.in.web;

import com.swiftly.service.user.api.dto.FieldErrorDto;
import com.swiftly.service.user.api.dto.ValidationErrorResponse;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles EmailAlreadyInUseException, which is thrown by the UserService when
     * a user attempts to register with an email that is already in use.
     *
     * @param e the EmailAlreadyInUseException thrown during user registration
     * @return a Mono containing an ErrorResponse with the error details
     */
    @ExceptionHandler(EmailAlreadyInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ErrorResponse> handleEmailExists(EmailAlreadyInUseException e) {
        return Mono.just(new ErrorResponse(e.getMessage(), "EMAIL_ALREADY_IN_USE"));
    }

    /**
     * Handles InvalidCredentialsException, which is thrown by the UserService
     * when the user-provided credentials are invalid.
     *
     * @param ex the InvalidCredentialsException thrown during login
     * @return a Mono containing an ErrorResponse with the error details
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        // Return a generic error response with the exception's message
        return Mono.just(new ErrorResponse(
                "INVALID_CREDENTIALS",
                ex.getMessage()
        ));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ValidationErrorResponse> handleValidationException(WebExchangeBindException exception) {
        var errors = exception.getFieldErrors().stream()
                .map(fieldError -> new FieldErrorDto(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        return Mono.just(new ValidationErrorResponse(
                Instant.now().toString(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                errors
        ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return Mono.just(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Catch Spring’s own 400 Type‐mismatch exception (thrown when a PathVariable cannot
     * be converted to the target type, e.g. UUID.fromString fails) and return a clean,
     * 400 BAD_REQUEST with our ErrorResponse shape.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadRequest(ResponseStatusException ex) {
        // only intercept BAD_REQUEST type‐mismatch errors
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST && "Type mismatch.".equals(ex.getReason())) {
            ErrorResponse err = new ErrorResponse(
                    ex.getMostSpecificCause().getMessage(),
                    ex.getReason()
            );
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(err));
        }
        // for other ResponseStatusExceptions, re‐throw so Spring handles it
        return Mono.error(ex);
    }

    /**
     * Handles DataIntegrityViolationException, which is thrown by Spring Data R2dbc
     * when the database operation fails due to a constraint violation, such as
     * inserting a null value into a non-nullable column, or inserting a value
     * that is not valid according to the table's constraints.
     *
     * @param ex the DataIntegrityViolationException thrown during database operations
     * @return a Mono containing an ErrorResponse with the error details
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Create an error response with the database error message
        return Mono.just(new ErrorResponse(
                "Invalid data: " + ex.getMostSpecificCause().getMessage(),
                "DATA_INTEGRITY_VIOLATION"
        ));
    }

    /**
     * Handles R2DBC-level errors such as connection refused, bad SQL, or timeouts.
     *
     * @param ex the R2dbcException thrown during database operations
     * @return a Mono containing an ErrorResponse with the error details
     */
    @ExceptionHandler(io.r2dbc.spi.R2dbcException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleR2dbc(io.r2dbc.spi.R2dbcException ex) {
        // Create an error response with the database error message
        return Mono.just(new ErrorResponse(
                "Database error: " + ex.getMessage(),
                "DATABASE_ERROR"
        ));
    }

    /**
     * Fallback handler for any uncaught exceptions.
     * This method is invoked when an exception is not explicitly handled
     * by other exception handlers within the application.
     *
     * @param ex the uncaught exception
     * @return a Mono containing an ErrorResponse with a generic error message
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponse> handleGeneric(Exception ex) {
        // Return a generic error response with the exception's message
        return Mono.just(new ErrorResponse(
                "Unexpected error: " + ex.getMessage(),
                "INTERNAL_SERVER_ERROR"
        ));
    }

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private String message;
        private String code;
    }

}
