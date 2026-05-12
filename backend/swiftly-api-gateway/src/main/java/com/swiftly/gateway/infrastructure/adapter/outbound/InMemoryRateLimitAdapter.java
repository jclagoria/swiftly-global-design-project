package com.swiftly.gateway.infrastructure.adapter.outbound;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import com.swiftly.gateway.domain.port.outbound.RateLimitStorePort;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory adapter that implements the RateLimitStorePort using Bucket4j.
 * Initializes buckets at application startup and provides current usage stats.
 */
@Slf4j
@ApplicationScoped
public class InMemoryRateLimitAdapter implements RateLimitStorePort {

    @ConfigProperty(name = "rate-limit.default.limit", defaultValue = "1000")
    int defaultLimit;

    @ConfigProperty(name = "rate-limit.default.window.seconds", defaultValue = "60")
    long defaultWindowSecs;

    @Inject
    MeterRegistry meterRegistry;

    // holds our in‐memory buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    // holds the original Bandwidth for each bucket so we can report capacity
    private final Map<String, Bandwidth> bandwidths = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        List<String> endpoints = List.of("/health", "/status", "/rate-limits");
        for (String path : endpoints) {
            // 1) build your Bandwidth via the new builder API
            Bandwidth limit = Bandwidth.builder()
                    .capacity(defaultLimit)                                       // replaces classic(defaultLimit,…)
                    .refillGreedy(defaultLimit, Duration.ofSeconds(defaultWindowSecs))  // replaces Refill.greedy(…)
                    .build();

            // 2) wire that Bandwidth into a new in‐memory Bucket
            Bucket bucket = Bucket.builder()
                    .addLimit(limit)
                    .build();

            // 3) store both so listRateLimits() can report capacity & availableTokens
            buckets.put(path, bucket);
            bandwidths.put(path, limit);
        }

        log.info(
                "Initialized rate-limit buckets for endpoints={} with defaultLimit={} and defaultWindowSecs={}",
                endpoints, defaultLimit, defaultWindowSecs
        );
    }

    @Override
    public Uni<List<RateLimitInfo>> listRateLimits() {
        return Uni.createFrom().deferred(() -> {
                    try {
                        // Build the list and register a gauge per route
                        List<RateLimitInfo> infos = buckets.entrySet().stream()
                                .map(e -> {
                                    String path = e.getKey();
                                    Bucket bucket = e.getValue();
                                    Bandwidth b = bandwidths.get(path);

                                    // Map into domain object
                                    RateLimitInfo info = new RateLimitInfo(
                                            path,
                                            (int) b.getCapacity(),
                                            (int) bucket.getAvailableTokens(),
                                            defaultWindowSecs
                                    );

                                    // Runtime metric: available tokens per route
                                    meterRegistry.gauge(
                                            "ratelimits.available_tokens",
                                            Tags.of("route", path),
                                            info,
                                            RateLimitInfo::getRemaining
                                    );

                                    return info;
                                })
                                .collect(Collectors.toList());

                        return Uni.createFrom().item(infos);

                    } catch (Exception ex) {
                        // Log and propagate any unexpected failure
                        log.error("Failed to list rate limits", ex);
                        return Uni.createFrom().failure(ex);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onFailure().invoke(err -> log.error("Error listing rate limits", err));
    }
}
