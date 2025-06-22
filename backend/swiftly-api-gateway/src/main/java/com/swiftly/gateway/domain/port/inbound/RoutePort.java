package com.swiftly.gateway.domain.port.inbound;

import com.swiftly.gateway.domain.model.RouteMetadata;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface RoutePort {

    Uni<List<RouteMetadata>> getActiveRoutes();

}
