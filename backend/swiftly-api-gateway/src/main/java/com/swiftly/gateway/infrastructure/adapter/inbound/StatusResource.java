package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.port.inbound.SystemStatusPort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.smallrye.mutiny.Uni;

@Path("/status")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Tag(name = "Status", description = "Endpoints for gateway health checks")
public class StatusResource {

    private final SystemStatusPort systemStatusPort;

    @Inject
    public StatusResource(SystemStatusPort systemStatusPort) {
        this.systemStatusPort = systemStatusPort;
    }

    @GET
    @Operation(summary = "Get system status")
    @APIResponse(
            responseCode = "200",
            description = "Successfully retrieved system status",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SystemStatus.class))
    )
    @APIResponse(
            responseCode = "503",
            description = "Failed to retrieve system status",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorResponse.class))
    )
    public Uni<Response> status() {
        return systemStatusPort.getSystemStatus()
                .onItem().transform(status ->
                        Response.ok(status).build())
                .onFailure().recoverWithItem(throwable -> {
                    log.error("Error fetching system status", throwable);
                    Map<String, String> error = Map.of(
                            "error", throwable.getMessage() != null ? throwable.getMessage() : "unknown error"
                    );
                    return Response
                            .status(Response.Status.SERVICE_UNAVAILABLE)
                            .entity(error)
                            .build();
                });
    }
}
