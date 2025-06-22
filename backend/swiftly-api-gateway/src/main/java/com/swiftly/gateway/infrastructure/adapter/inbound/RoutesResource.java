package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.port.inbound.RoutePort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Slf4j
@Path("/routes")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Gateway Routes", description = "Dynamic route management")
public class RoutesResource {

    private final RoutePort routePort;

    @Inject
    public  RoutesResource(RoutePort routePort) {
        this.routePort = routePort;
    }

    @GET
    @Operation(summary = "List active dynamic routes")
    public Uni<Response> listRoutes() {
        log.info("Received request to list active routes.");
        return routePort.getActiveRoutes()
                .map(routeMetadata -> {
                    log.info("Returning {} active routes", routeMetadata.size());
                    return Response.ok(routeMetadata).build();
                })
                .onFailure().recoverWithItem(ex -> {
                    log.error("Failed to retrieve active routes", ex);
                    return Response.serverError()
                            .entity("Failed to retrieve routes: " + ex.getMessage())
                            .build();
                });
    }

}
