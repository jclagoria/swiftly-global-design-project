package com.swiftly.gateway.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RouteMetadata {

    private final String routeId;
    private final String pathPattern;
    private final String targetService;
    private final String status;
    private final Map<String, Object> metadata;
    private final Instant lastUpdated;
    private final Optional<Long> requestCount;
    private final Optional<Double> errorRate;

}
