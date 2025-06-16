package com.swiftly.service.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileModel {

    private UUID    id;
    private String  email;
    private String  firstName;
    private String  lastName;
    private String phone;
    private String address;
    private String locale;
    private String timezone;
    private Instant createdAt;
    private Instant updatedAt;

}