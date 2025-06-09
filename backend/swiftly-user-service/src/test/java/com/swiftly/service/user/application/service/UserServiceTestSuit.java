package com.swiftly.service.user.application.service;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        UserServiceRegisterTest.class,
        UserServiceLogoutTest.class,
        UserServiceLoginTest.class,
        UserServiceGetUserProfileTest.class,
        UserServiceUpdateUserProfileTest.class
})
public class UserServiceTestSuit {
}
