package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserProfileMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.application.port.in.UserManagementService;
import com.swiftly.service.user.domain.exception.EmailAlreadyInUseException;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import com.swiftly.service.user.domain.model.UserModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserPersistenceMapper userMapper;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper profileMapper;
    private final UserServiceUtils userServiceUtils;

    @Transactional
    @Override
    public Mono<UserModel> register(RegisterUserRequest request) {
        return userServiceUtils.validateEmailExists(request.getEmail(), userRepository)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new EmailAlreadyInUseException(request.getEmail()));
                    }
                    var userEntity = userServiceUtils.createUserEntity(request);
                    return userRepository.save(userEntity)
                            .doOnSuccess(savedUser ->
                                    log.info("User registered successfully: {}", request.getEmail()))
                            .doOnError(error ->
                                    log.error("Error registering user for email {}: {}", request.getEmail(), error.getMessage()));
                })
                .flatMap(savedUser -> {
                    var userProfile = profileMapper.toEntity(savedUser.getId(), request);
                    return userProfileRepository.insert(userProfile)
                            .thenReturn(savedUser);
                })
                .map(userMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteUser(UUID userId) {
        return userRepository.findByIdAndDeletedIsFalse(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> {
                    user.setDeleted(true);
                    user.setDeletedAt(Instant.now());
                    return userRepository.save(user).then();
                });
    }

}
