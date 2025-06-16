package com.swiftly.service.user.adapter.out.persistence.mapper;

import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.domain.model.UserModel;
import com.swiftly.service.user.domain.model.UserProfileModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

    @Mapping(source = "user.id",        target = "id")
    @Mapping(source = "user.email",     target = "email")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName",  target = "lastName")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "profile.phone",     target = "phone")
    @Mapping(source = "profile.address",   target = "address")
    @Mapping(source = "profile.locale",    target = "locale")
    @Mapping(source = "profile.timezone",  target = "timezone")
    @Mapping(source = "profile.updatedAt", target = "updatedAt")
    UserProfileModel toModel(UserModel user, UserProfileEntity profile);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "phone", source = "request.phone")
    @Mapping(target = "address", source = "request.address")
    @Mapping(target = "locale", source = "request.locale", defaultValue = "en-US")
    @Mapping(target = "timezone", source = "request.timezone", defaultValue = "UTC")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfileEntity toEntity(UUID userId, RegisterUserRequest request);
}
