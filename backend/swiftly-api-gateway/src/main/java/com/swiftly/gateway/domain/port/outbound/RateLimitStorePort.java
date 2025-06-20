package com.swiftly.gateway.domain.port.outbound;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Outbound port for persisting and accessing rate-limit stores.
 */
public interface RateLimitStorePort {

    /**
     * Retrieves the list of rate limit information.
     *
     * @return A Uni containing a list of RateLimitInfo objects, each representing a configured rate limit.
     */
    Uni<List<RateLimitInfo>> listRateLimits();

}
