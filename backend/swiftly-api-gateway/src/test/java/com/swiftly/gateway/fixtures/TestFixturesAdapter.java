package com.swiftly.gateway.fixtures;

import com.swiftly.gateway.domain.model.HealthStatus;
import com.swiftly.gateway.domain.model.RateLimitInfo;
import com.swiftly.gateway.domain.model.RegistryStatus;
import com.swiftly.gateway.domain.model.SystemStatus;
import com.swiftly.gateway.infrastructure.client.DownstreamHealthResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestFixturesAdapter {

    private TestFixturesAdapter() { /* prevent instantiation */ }

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

    /**
     * @param count number of dummy services
     * @return a SystemStatus with gateway/registry UP and 'count' services
     */
    public static SystemStatus sampleSystemStatus(int count) {
        RegistryStatus registry = RegistryStatus.builder()
                .status("UP")
                .details(Map.of("services", count))
                .build();

        return SystemStatus.builder()
                .gateway("UP")
                .registry(registry)
                .servicesCount(count)
                .build();
    }

    /**
     * @return a RuntimeException simulating registry failure
     */
    public static RuntimeException sampleException() {
        return new RuntimeException("Service discovery failed");
    }

    public static RateLimitInfo sampleRateLimit() {
        return RateLimitInfo.builder()
                .route("/test")
                .limit(100)
                .remaining(80)
                .windowSecs(60L)
                .build();
    }

    public static List<RateLimitInfo> sampleRateLimits() {
        return List.of(
                sampleRateLimit(),
                RateLimitInfo.builder()
                        .route("/test2")
                        .limit(200)
                        .remaining(190)
                        .windowSecs(120L)
                        .build()
        );
    }

    public static List<RateLimitInfo> emptyRateLimits() {
        return Collections.emptyList();
    }

    public static List<RateLimitInfo> rateLimitsOfSize(int n) {
        List<RateLimitInfo> list = new ArrayList<>(n);
        IntStream.rangeClosed(1, n)
                .forEach(i ->
                        list.add(RateLimitInfo.builder()
                                .route("/route-" + i)
                                .limit(100 * i)
                                .remaining(100 * i - 10)
                                .windowSecs(60L * i)
                                .build())
                );
        return list;
    }

    public static java.util.stream.Stream<java.util.List<RateLimitInfo>> listsProvider() {
        return java.util.stream.Stream.of(
                sampleRateLimits(),
                emptyRateLimits(),
                rateLimitsOfSize(3)
        );
    }

    /** Must match exactly the endpoints in your adapter’s init() method. */
    public static final List<String> ENDPOINTS = List.of("/health", "/status", "/rate-limits");

    /** Build a list of RateLimitInfo for the given limit/window. */
    public static List<RateLimitInfo> rateLimitInfos(int limit, long windowSecs) {
        return ENDPOINTS.stream()
                .map(route -> rateLimitInfo(route, limit, windowSecs))
                .collect(Collectors.toList());
    }

    /** Build a single RateLimitInfo with remaining == limit (fresh bucket). */
    public static RateLimitInfo rateLimitInfo(String route, int limit, long windowSecs) {
        return new RateLimitInfo(route, limit, limit, windowSecs);
    }


}
