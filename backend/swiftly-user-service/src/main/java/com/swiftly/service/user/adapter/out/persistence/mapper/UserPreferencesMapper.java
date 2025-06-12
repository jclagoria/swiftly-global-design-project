package com.swiftly.service.user.adapter.out.persistence.mapper;

import com.swiftly.service.user.adapter.out.persistence.entities.mongo.UserPreferencesEntity;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel     = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserPreferencesMapper {
    UserPreferencesModel toDomain(UserPreferencesEntity entity);
}
