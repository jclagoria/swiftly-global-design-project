package com.swiftly.service.user.application.service;


import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.config.security.JwtTokenProvider;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.exception.InvalidCredentialsException;
import com.swiftly.service.user.domain.model.UserModel;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserPersistenceMapper mapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserServiceImpl userService;

    private EasyRandom easyRandom;
    private RegisterUserRequest request;
    private UserEntity toSave;
    private UserEntity savedEntity;
    private UserModel expectedModel;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();

        // generate a random registration request
        String randomEmail = easyRandom.nextObject(String.class) + "@test.com";
        request = RegisterUserRequest.builder()
                .email(randomEmail)
                .password(easyRandom.nextObject(String.class))
                .firstName(easyRandom.nextObject(String.class))
                .lastName(easyRandom.nextObject(String.class))
                .build();

        // prepare the entity that mapper.toEntity will return
        toSave = easyRandom.nextObject(UserEntity.class);
        toSave.setEmail(request.getEmail());
        toSave.setFirstName(request.getFirstName());
        toSave.setLastName(request.getLastName());
        // ensure the passwordHash is set to the encoded value
        toSave.setPasswordHash("ENCODED");

        // prepare what repository.save should emit
        savedEntity = easyRandom.nextObject(UserEntity.class);
        savedEntity.setEmail(request.getEmail());
        savedEntity.setFirstName(request.getFirstName());
        savedEntity.setLastName(request.getLastName());
        savedEntity.setPasswordHash("ENCODED");
        savedEntity.setId(UUID.randomUUID());

        // prepare the domain model the mapper.toDomain will return
        expectedModel = UserModel.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash("ENCODED")
                .build();
    }

    @Test
    void register_whenEmailAlreadyExists_shouldError() {
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(Mono.just(true));

        Mono<UserModel> mono = userService.register(request);

        StepVerifier.create(mono)
                .expectErrorMatches(ex ->
                        ex instanceof EmailAlreadyInUseException
                                && ex.getMessage().contains(request.getEmail())
                )
                .verify();

        verify(userRepository).existsByEmail(request.getEmail());
        verifyNoMoreInteractions(userRepository, passwordEncoder, mapper);
    }

    @Test
    void register_whenEmailNotExists_shouldSaveAndReturnModel() {
        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(Mono.just(false));
        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("ENCODED");
        when(mapper.toEntity(any(UserModel.class)))
                .thenReturn(toSave);
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(Mono.just(savedEntity));
        when(mapper.toDomain(savedEntity))
                .thenReturn(expectedModel);

        Mono<UserModel> mono = userService.register(request);

        StepVerifier.create(mono)
                .expectNextMatches(model ->
                        model.getEmail().equals(expectedModel.getEmail()) &&
                                model.getFirstName().equals(expectedModel.getFirstName()) &&
                                model.getPasswordHash().equals(expectedModel.getPasswordHash())
                )
                .verifyComplete();

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(captor.capture());
        verify(mapper).toEntity(any(UserModel.class));
        verify(mapper).toDomain(savedEntity);

        UserEntity passed = captor.getValue();
        assertThat(passed.getEmail()).isEqualTo(request.getEmail());
        // now the passwordHash is correctly the encoded value
        assertThat(passed.getPasswordHash()).isEqualTo("ENCODED");
    }

    @Test
    void login_whenCredentialsAreValid_shouldReturnToken() {
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        savedEntity.setEmail(email); // ensure savedEntity has the same email
        savedEntity.setPasswordHash("HASHED_PASSWORD");

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(savedEntity));
        when(passwordEncoder.matches(password, "HASHED_PASSWORD")).thenReturn(true);
        when(jwtTokenProvider.createToken(email)).thenReturn("JWT_TOKEN");

        // When
        Mono<String> result = userService.login(loginRequest);

        // Then
        StepVerifier.create(result)
                .expectNext("JWT_TOKEN")
                .verifyComplete();

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, "HASHED_PASSWORD");
        verify(jwtTokenProvider).createToken(email);
    }

    @Test
    void login_whenUserNotFound_shouldThrowInvalidCredentialsException() {
        // Given
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Mono.empty());

        // When
        Mono<String> result = userService.login(loginRequest);

        // Then
        StepVerifier.create(result)
                .expectError(InvalidCredentialsException.class)
                .verify();

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    void login_whenPasswordDoesNotMatch_shouldThrowInvalidCredentialsException() {
        // Given
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        savedEntity.setEmail(email);
        savedEntity.setPasswordHash("HASHED_PASSWORD");

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(savedEntity));
        when(passwordEncoder.matches(password, "HASHED_PASSWORD")).thenReturn(false);

        // When
        Mono<String> result = userService.login(loginRequest);

        // Then
        StepVerifier.create(result)
                .expectError(InvalidCredentialsException.class)
                .verify();

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, "HASHED_PASSWORD");
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void login_whenUnexpectedErrorOccurs_shouldLogError() {
        // Given
        String email = easyRandom.nextObject(String.class) + "@test.com";
        String password = easyRandom.nextObject(String.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        when(userRepository.findByEmail(email))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        // When
        Mono<String> result = userService.login(loginRequest);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }


}
