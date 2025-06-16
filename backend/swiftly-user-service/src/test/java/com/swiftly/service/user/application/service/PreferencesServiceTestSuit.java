package com.swiftly.service.user.application.service;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        PreferencesServiceRegisterTest.class,
        PreferencesServiceLogoutTest.class,
        PreferencesServiceLoginTest.class,
        UserServiceGetPreferencesProfileTest.class,
        UserServiceUpdatePreferencesProfileTest.class,
        UserServiceGetPreferencesPreferencesTest.class,
        UserServiceUpdatePreferencesPreferencesTest.class,

})
public class PreferencesServiceTestSuit {
}
