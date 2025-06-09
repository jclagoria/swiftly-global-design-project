package com.swiftly.service.user.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security configuration for encoding passwords.
 */
@Configuration
public class SecurityConfigEncoding {


    /**
     * Provides a {@link PasswordEncoder} to be used for encoding passwords.
     * This implementation uses the BCrypt algorithm.
     * @return a {@link PasswordEncoder} instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
