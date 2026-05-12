package com.swiftly.gateway.domain.port.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;

/**
 * Outbound port for fetching a single service's OpenAPI document.
 * Implementations may cache or retry as needed.
 */
public interface OpenApiStorePort {

    /**
     * Fetch the OpenAPI spec for the given service name.
     *
     * @param serviceName identifier of the target service
     * @return a Uni emitting the service's OpenAPI JsonNode
     */
    Uni<JsonNode> fetchOpenApi(String serviceName);

}
