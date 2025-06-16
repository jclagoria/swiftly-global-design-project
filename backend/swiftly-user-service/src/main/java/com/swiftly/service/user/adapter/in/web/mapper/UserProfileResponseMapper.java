package com.swiftly.service.user.adapter.in.web.mapper;

import com.swiftly.service.user.api.dto.UserProfileResponse;
import com.swiftly.service.user.domain.model.UserProfileModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel     = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface UserProfileResponseMapper {

    @Mapping(source = "id", target = "userId")
    UserProfileResponse toResponse(UserProfileModel model);

}
