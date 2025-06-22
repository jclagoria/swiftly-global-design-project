package com.swiftly.gateway.infrastructure.adapter.outbound;

import com.swiftly.gateway.domain.model.RouteMetadata;
import com.swiftly.gateway.domain.port.outbound.RouteRegistryAdapter;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class StorkRouteRegistryAdapter implements RouteRegistryAdapter {

    private static final String DEFAULT_STATUS = "active";
    private static final String METADATA_KEY_DISCOVERY = "discovery";
    private static final String METADATA_VALUE_STORK = "stork";

    @Override
    public Uni<List<RouteMetadata>> findAllRoutes() {
        return Uni.createFrom().item(() -> {
            log.info("Fetching service names from Stork registry...");
            Instant now = Instant.now();

            List<RouteMetadata> routes = Stork.getInstance()
                    .getServices()
                    .keySet()
                    .stream()
                    .map(serviceName -> {
                        String pathPattern = "/" + serviceName + "/*";
                        RouteMetadata metadata = new RouteMetadata(
                                serviceName,
                                pathPattern,
                                serviceName,
                                DEFAULT_STATUS,
                                Map.of(METADATA_KEY_DISCOVERY, METADATA_VALUE_STORK),
                                now,
                                Optional.of(0L),
                                Optional.of(0.0)
                        );
                        log.debug("Discovered service '{}': {}", serviceName, metadata);
                        return metadata;
                    })
                    .collect(Collectors.toList());

            log.info("Total services discovered: {}", routes.size());
            return routes;
        }).onFailure().invoke(ex -> log.error("Failed to retrieve routes from Stork registry", ex));
    }
}
