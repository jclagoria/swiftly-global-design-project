package com.swiftly.gateway.infrastructure.adapter.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.swiftly.gateway.domain.port.outbound.OpenApiStorePort;
import com.swiftly.gateway.infrastructure.client.DownstreamOpenApiClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;

@Slf4j
@ApplicationScoped
public class StorkOpenApiAdapter implements OpenApiStorePort {

    private final DownstreamOpenApiClient downstreamOpenApiClient;
    private Uni<JsonNode> cachedSpec;

    @Inject
    public StorkOpenApiAdapter(@RestClient DownstreamOpenApiClient downstreamOpenApiClient) {
        this.downstreamOpenApiClient = downstreamOpenApiClient;
    }

    @Override
    public Uni<JsonNode> fetchOpenApi(String serviceName) {
        if (cachedSpec == null) {
            synchronized (this) {
                if (cachedSpec == null) {
                    cachedSpec = downstreamOpenApiClient.getOpenApi()
                            .onFailure().invoke(e -> log.error("downstream error", e))
                            .memoize().forFixedDuration(Duration.ofHours(1));
                }
            }
        }
        return cachedSpec;
    }
}
