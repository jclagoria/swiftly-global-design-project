package com.swiftly.gateway.domain.port.inbound;

import com.swiftly.gateway.domain.model.HealthStatus;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface HealthCheckPort {

    /**
     * Executes all health checks.
     *
     * @return A Uni containing all health checks.
     */
    Uni<List<HealthStatus>> checkAll();

}
