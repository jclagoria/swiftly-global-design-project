package com.swiftly.gateway.domain.port.outbound;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface ServiceDiscoveryPort {

    /**
     * Gets the list of registered services.
     *
     * @return A Uni containing the list of registered services.
     */
    Uni<List<String>> getRegisteredServices();

}
