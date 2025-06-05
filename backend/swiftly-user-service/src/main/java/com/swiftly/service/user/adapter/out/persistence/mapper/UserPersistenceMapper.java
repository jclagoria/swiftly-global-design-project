package com.swiftly.service.user.adapter.out.persistence.mapper;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.domain.model.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel     = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserPersistenceMapper {
    /**
     * Converts a {@link UserModel} to a {@link UserEntity}.
     * When inserting, we do NOT set id or createdAt (Postgres will do it).
     * @param domain the user model to convert
     * @return the converted user entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    UserEntity toEntity(UserModel domain);

    /**
     * Converts a {@link UserEntity} to a {@link UserModel}.
     *
     * @param entity the user entity to convert
     * @return the converted user model
     */
    UserModel toDomain(UserEntity entity);
}
