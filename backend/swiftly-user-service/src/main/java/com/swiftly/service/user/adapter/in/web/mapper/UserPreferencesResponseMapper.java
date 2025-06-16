package com.swiftly.service.user.adapter.in.web.mapper;

import com.swiftly.service.user.api.dto.UserPreferenceResponse;
import com.swiftly.service.user.domain.model.UserPreferencesModel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel     = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserPreferencesResponseMapper {

    UserPreferenceResponse toResponse(UserPreferencesModel model);

}
