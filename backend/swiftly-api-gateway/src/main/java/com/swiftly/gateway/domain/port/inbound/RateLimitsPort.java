package com.swiftly.gateway.domain.port.inbound;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Inbound port for retrieving rate-limit information from the domain.
 */
public interface RateLimitsPort {

    /**
     * Retrieves the list of configured rate limits.
     *
     * @return A Uni containing the list of RateLimitInfo objects, each representing a configured rate limit.
     */
    Uni<List<RateLimitInfo>> getRateLimits();

}
