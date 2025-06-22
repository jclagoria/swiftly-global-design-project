package com.swiftly.gateway.domain.port.outbound;

import com.swiftly.gateway.domain.model.RouteMetadata;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface RouteRegistryAdapter {

    Uni<List<RouteMetadata>> findAllRoutes();

}
