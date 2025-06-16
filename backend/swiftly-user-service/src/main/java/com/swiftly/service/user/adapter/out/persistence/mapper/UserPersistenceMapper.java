package com.swiftly.service.user.adapter.out.persistence.mapper;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.domain.model.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel     = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserPersistenceMapper {

    /**
     * Converts a {@link UserEntity} to a {@link UserModel}.
     *
     * @param entity the user entity to convert
     * @return the converted user model
     */
    UserModel toDomain(UserEntity entity);
}
