package com.swiftly.gateway.application.usecase;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import com.swiftly.gateway.domain.port.inbound.RateLimitsPort;
import com.swiftly.gateway.domain.port.outbound.RateLimitStorePort;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Application service (use-case) for retrieving rate-limit information.
 * Delegates to an inbound port and records metrics and logs failures.
 */
@ApplicationScoped
@Slf4j
public final class RateLimitsUseCase implements RateLimitsPort {

    private final RateLimitStorePort rateLimitStorePort;
    private final MeterRegistry meterRegistry;

    @Inject
    public RateLimitsUseCase(RateLimitStorePort rateLimitStorePort,
                             MeterRegistry meterRegistry) {
        this.rateLimitStorePort = rateLimitStorePort;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Get the list of configured rate limits.
     *
     * @return A Uni containing the list of configured rate limits.
     */
    @Override
    public Uni<List<RateLimitInfo>> getRateLimits() {
        // Simply forward the call to the injected port
        return rateLimitStorePort.listRateLimits()
                .invoke(list ->
                        meterRegistry.gauge("ratelimits.count", list, List::size))
                .onFailure().invoke(t -> log.error("Failed to fetch rate limits", t));
    }
}
