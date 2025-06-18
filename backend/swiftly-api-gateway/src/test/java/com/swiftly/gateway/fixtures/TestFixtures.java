package com.swiftly.gateway.fixtures;

import com.swiftly.gateway.domain.model.HealthStatus;
import com.swiftly.gateway.domain.model.Status;
import com.swiftly.gateway.infrastructure.client.DownstreamHealthResponse;

import java.util.List;
import java.util.Map;

public class TestFixtures {

    /**
     * A sample list of service names for happy-path scenarios
     */
    public static List<String> services() {
        return List.of("serviceA", "serviceB");
    }

    /**
     * A downstream response indicating an UP status with deterministic details
     */
    public static DownstreamHealthResponse healthyDownstreamResponse() {
        return new DownstreamHealthResponse(
                "UP",
                Map.of("detailKey", "detailValue")
        );
    }

    /**
     * A domain HealthStatus built from the healthy downstream response
     */
    public static HealthStatus healthyHealthStatus(String service) {
        return new HealthStatus(
                service,
                "UP",
                Map.of("detailKey", "detailValue")
        );
    }

    public static HealthStatus upStatus(String name) {
        // Fixed timestamp for deterministic tests
        return new HealthStatus(name, "UP", Map.of("timestamp", "2025-01-01T00:00:00Z"));
    }

    public static HealthStatus downStatus(String name) {
        // Fixed error and timestamp for deterministic tests
        return new HealthStatus(name, "DOWN", Map.of(
                "error", "failure",
                "timestamp", "2025-01-01T00:00:00Z"
        ));
    }
}
