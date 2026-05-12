package com.swiftly.gateway.infrastructure.adapter;

import com.swiftly.gateway.domain.model.RouteMetadata;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ResourceFixtures {
    /** A fixed instant for deterministic JSON. */
    public static Instant fixedInstant() {
        return Instant.parse("2025-06-22T00:00:00Z");
    }

    /**
     * Build a RouteMetadata with given id and default “active” status.
     */
    public static RouteMetadata route(String id) {
        return RouteMetadata.builder()
                .routeId(id)
                .pathPattern("/" + id + "/*")
                .targetService(id)
                .status("active")
                .metadata(Map.of("source", "test"))
                .lastUpdated(fixedInstant())
                .requestCount(Optional.of(42L))
                .errorRate(Optional.of(0.0))
                .build();
    }

    /** Build a list of RouteMetadata with IDs route0…routeN–1. */
    public static List<RouteMetadata> routeList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> route("route" + i))
                .collect(Collectors.toList());
    }
}
