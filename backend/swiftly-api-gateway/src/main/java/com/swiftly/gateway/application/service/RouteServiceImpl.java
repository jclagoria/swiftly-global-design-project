package com.swiftly.gateway.application.service;

import com.swiftly.gateway.domain.model.RouteMetadata;
import com.swiftly.gateway.domain.port.outbound.RouteRegistryAdapter;
import com.swiftly.gateway.domain.port.inbound.RoutePort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
public class RouteServiceImpl implements RoutePort {

    private static final String ACTIVE_STATUS = "active";

    private final RouteRegistryAdapter routeRegistryAdapter;

    @Inject
    public RouteServiceImpl(RouteRegistryAdapter routeRegistryAdapter) {
        this.routeRegistryAdapter = routeRegistryAdapter;
    }

    @Override
    public Uni<List<RouteMetadata>> getActiveRoutes() {
        log.info("Request received to fetch active routes.");
        return routeRegistryAdapter.findAllRoutes()
                .onFailure().invoke(ex ->
                        log.error("Failed to fetch routes from RouteRegistryAdapter", ex))
                .map(routes -> {
                    List<RouteMetadata> activeRoutes = routes.stream()
                            .filter(route ->
                                    ACTIVE_STATUS.equalsIgnoreCase(route.getStatus()))
                            .toList();
                    log.info("Found {} active routes out of {} total.", activeRoutes.size(), routes.size());
                    return activeRoutes;
                });
    }
}
