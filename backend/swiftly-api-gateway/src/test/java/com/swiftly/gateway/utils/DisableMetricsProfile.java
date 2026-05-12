package com.swiftly.gateway.utils;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class DisableMetricsProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.smallrye-metrics.enabled", "false",
                "quarkus.micrometer.enabled",     "false"
        );
    }
}
