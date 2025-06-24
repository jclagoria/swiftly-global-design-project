package com.swiftly.gateway.domain.port.inbound;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;

/**
 * Inbound port for retrieving the combined OpenAPI specification.
 * Implementations provide the orchestration necessary to aggregate
 * specs from downstream services.
 */
public interface OpenApiPort {

    /**
     * Retrieves the aggregated OpenAPI specification for all services.
     *
     * @return a Uni emitting the merged OpenAPI spec as a JsonNode
     */
    Uni<JsonNode> getAggregateOpenApi();
}
