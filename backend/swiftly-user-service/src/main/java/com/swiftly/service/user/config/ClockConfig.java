package com.swiftly.service.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /**
     * Exposes a system‐UTC Clock for injection anywhere you need the current time.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
