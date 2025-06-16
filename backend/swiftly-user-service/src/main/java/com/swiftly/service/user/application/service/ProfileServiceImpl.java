package com.swiftly.service.user.application.service;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.swiftly.service.user.adapter.out.persistence.mapper.UserProfileMapper;
import com.swiftly.service.user.adapter.out.persistence.repository.UserProfileRepository;
import com.swiftly.service.user.adapter.out.persistence.repository.UserRepository;
import com.swiftly.service.user.api.dto.UpdateUserRequest;
import com.swiftly.service.user.application.port.in.ProfileService;
import com.swiftly.service.user.domain.exception.UserNotFoundException;
import com.swiftly.service.user.domain.model.UserProfileModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private final UserPersistenceMapper userMapper;
    private final UserProfileMapper profileMapper;

    @Override
    public Mono<UserProfileModel> getUserProfile(UUID userId) {
        return userRepository.findByIdAndDeletedIsFalse(userId)                                    // 1️⃣ fetch user
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .map(userMapper::toDomain)                                             // 2️⃣ map to UserModel
                .flatMap(userModel ->
                        userProfileRepository.findById(userId)                             // 3️⃣ fetch profile
                                .defaultIfEmpty(new UserProfileEntity(
                                        userId, null, null, "en-US", "UTC", null, null))
                                .map(profileEntity ->
                                        profileMapper.toModel(userModel, profileEntity)            // 4️⃣ combine them
                                )
                );
    }

    @Override
    public Mono<Void> updateUserProfile(UUID userId, UpdateUserRequest updateUserRequest) {
        // Fetch the user from the database
        Mono<UserEntity> userMono = userRepository.findByIdAndDeletedIsFalse(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)));

        // Fetch the profile from the database
        Mono<UserProfileEntity> profileMono = userProfileRepository.findById(userId);

        return Mono.zip(userMono, profileMono)
                .flatMap(tuple -> {
                    UserEntity user = tuple.getT1();
                    UserProfileEntity profile = tuple.getT2();

                    // Patch the core user
                    user.setFirstName(updateUserRequest.getFirstName());
                    user.setLastName(updateUserRequest.getLastName());

                    // Patch or update the profile
                    Optional.ofNullable(updateUserRequest.getPhone()).ifPresent(profile::setPhone);
                    Optional.ofNullable(updateUserRequest.getAddress()).ifPresent(profile::setAddress);
                    Optional.ofNullable(updateUserRequest.getLocale()).ifPresent(profile::setLocale);
                    Optional.ofNullable(updateUserRequest.getTimezone()).ifPresent(profile::setTimezone);
                    profile.setUpdatedAt(Instant.now());

                    // Save both at once, then complete
                    return Mono.zip(
                            userRepository.save(user),
                            userProfileRepository.save(profile)
                    ).then();
                });
    }
}
