package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserEntityRepository;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.application.port.in.UserService;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.model.UserModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPersistenceMapper mapper;

    @Override
    public Mono<UserModel> register(RegisterUserRequest request) {

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Attempt to register with already used email: {}", request.getEmail());
                        return Mono.error(new EmailAlreadyInUseException(request.getEmail()));
                    }
                    var user = mapper.toEntity(UserModel.builder()
                            .email(request.getEmail())
                            .passwordHash(encodedPassword)
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .build());

                    return userRepository
                            .save(user);
        })
                .doOnSuccess(savedUser ->
                        log.info("User registered successfully: {}", savedUser.getEmail()))
                .doOnError(error ->
                        log.error("Error registering user for email {}: {}", request.getEmail(), error.getMessage()))
                .map(mapper::toDomain);
    }
}
