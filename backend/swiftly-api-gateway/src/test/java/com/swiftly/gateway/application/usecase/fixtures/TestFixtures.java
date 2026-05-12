package com.swiftly.gateway.application.usecase.fixtures;

import com.swiftly.gateway.domain.model.RateLimitInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestFixtures {
    private TestFixtures() { /* prevent instantiation */ }

    /**
     * @param count number of dummy services to generate
     * @return List of service names: ["service-1", ..., "service-N"]
     */
    public static List<String> sampleServices(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> "service-" + i)
                .collect(Collectors.toList());
    }

    /**
     * @return a standard RuntimeException with a clear message
     * for simulating service discovery failures.
     */
    public static RuntimeException sampleException() {
        return new RuntimeException("Service discovery failed");
    }

    public static RateLimitInfo rateLimitInfo(String route, int limit, int remaining, long windowSecs) {
        return RateLimitInfo.builder()
                .route(route)
                .limit(limit)
                .remaining(remaining)
                .windowSecs(windowSecs)
                .build();
    }

    /** A simple “happy-path” single entry. */
    public static RateLimitInfo sampleRateLimit() {
        return rateLimitInfo("/test", 100, 80, 60L);
    }

    /** Two distinct entries. */
    public static List<RateLimitInfo> sampleRateLimits() {
        return List.of(
                sampleRateLimit(),
                rateLimitInfo("/test2", 200, 190, 120L)
        );
    }

    /**
     * Generate `count` entries with routes "/route-1", …, "/route-N",
     * all sharing the same limit/remaining/window.
     */
    public static List<RateLimitInfo> rateLimitsOfSize(int count) {
        List<RateLimitInfo> list = new ArrayList<>(count);
        IntStream.rangeClosed(1, count)
                .forEach(i ->
                        list.add(rateLimitInfo("/route-" + i, 100 * i, 100 * i - 10, 60L * i))
                );
        return list;
    }
}
