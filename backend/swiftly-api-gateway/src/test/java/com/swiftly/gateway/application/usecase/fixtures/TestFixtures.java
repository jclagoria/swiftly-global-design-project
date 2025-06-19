package com.swiftly.gateway.application.usecase.fixtures;

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
}
