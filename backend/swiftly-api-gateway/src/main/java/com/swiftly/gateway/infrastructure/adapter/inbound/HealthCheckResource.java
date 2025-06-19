package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.model.HealthStatus;
import com.swiftly.gateway.domain.model.Status;
import com.swiftly.gateway.domain.port.inbound.HealthCheckPort;
import com.swiftly.gateway.infrastructure.adapter.inbound.dto.HealthResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Path("/health")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckResource {

    private static final Logger LOGGER = Logger.getLogger(HealthCheckResource.class);

    private final HealthCheckPort healthUseCase;

    @Inject
    public HealthCheckResource(HealthCheckPort healthUseCase) {
        this.healthUseCase = healthUseCase;
    }

    @GET
    @Counted(name="health_requests_total", description="Total /health calls")
    @Timed(name="health_requests_duration", description="Time spent in /health")
    @Operation(summary = "Get gateway health overview")
    @APIResponse(responseCode = "200", description = "Overall UP or DOWN status",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    @APIResponse(responseCode = "500", description = "Internal error")
    public Uni<Response> health() {
        LOGGER.debug("Received /health request");

        return healthUseCase.checkAll()
                .onItem().invoke(list -> LOGGER.infof("Returning %d health checks", list.size()))
                .map(this::toOkResponse)
                .onFailure().invoke(t -> LOGGER.error("Failure in /health", t))
                .onFailure().recoverWithItem(this::toErrorResponse);
    }

    private Response toOkResponse(List<HealthStatus> list) {
        boolean anyDown = list.stream()
                .anyMatch(s -> Status.DOWN.name().equals(s.getStatus()));
        String overall = anyDown ? Status.DOWN.name() : Status.UP.name();
        HealthResponse payload = new HealthResponse(overall, list);
        return Response.ok(payload).build();
    }

    private Response toErrorResponse(Throwable t) {
        HealthStatus errorEntry = new HealthStatus(
                "health-endpoint",
                Status.DOWN.name(),
                Map.of(
                        "error", t.getMessage(),
                        "timestamp", Instant.now().toString()
                )
        );
        HealthResponse payload = new HealthResponse(Status.DOWN.name(), List.of(errorEntry));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(payload).build();
    }

}
