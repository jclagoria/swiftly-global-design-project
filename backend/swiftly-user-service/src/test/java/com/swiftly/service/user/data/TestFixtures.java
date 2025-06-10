package com.swiftly.service.user.data;

import com.swiftly.service.user.adapter.out.persistence.entities.UserEntity;
import com.swiftly.service.user.adapter.out.persistence.entities.UserProfileEntity;
import com.swiftly.service.user.api.dto.LoginRequest;
import com.swiftly.service.user.api.dto.RegisterUserRequest;
import com.swiftly.service.user.domain.model.UserModel;
import com.swiftly.service.user.domain.model.UserProfileModel;
import org.jeasy.random.EasyRandom;

public class TestFixtures {
    public static final EasyRandom rnd = new EasyRandom();

    public static RegisterUserRequest aRegisterRequest() {
        return RegisterUserRequest.builder()
                .email(rnd.nextObject(String.class) + "@test.com")
                .password(rnd.nextObject(String.class))
                .firstName(rnd.nextObject(String.class))
                .lastName(rnd.nextObject(String.class))
                .build();
    }

    public static UserEntityBuilder aEntity() {
        return UserEntityBuilder.builder().build();
    }

    public static UserModelBuilder aDomainModel() {
        return UserModelBuilder.builder().build();
    }

    public static UserEntity randomEntity() {
        return UserEntityBuilder.random().build();
    }

    public static UserModel randomModel() {
        return UserModelBuilder.random().build();
    }

    public static LoginRequest aLoginRequest(String email, String pwd) {
        return LoginRequest.builder()
                .email(email)
                .password(pwd)
                .build();
    }

    public static UserProfileEntityBuilder aProfileEntity() {
        return UserProfileEntityBuilder.builder().build();
    }

    public static UserProfileEntity randomProfileEntity() {
        return UserProfileEntityBuilder.random().build();
    }

    public static UserProfileModelBuilder aProfileModel() {
        return UserProfileModelBuilder.builder().build();
    }

    public static UserProfileModel randomProfileModel() {
        return UserProfileModelBuilder.random().build();
    }

    public static UpdateUserRequestBuilder aUpdateRequest() {
        return UpdateUserRequestBuilder.builder().build();
    }

    public static UpdateUserRequestBuilder randomUpdateRequest() {
        return UpdateUserRequestBuilder.random();
    }
}
