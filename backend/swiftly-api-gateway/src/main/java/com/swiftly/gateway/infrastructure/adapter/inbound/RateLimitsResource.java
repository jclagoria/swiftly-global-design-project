package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.port.inbound.RateLimitsPort;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import java.util.Map;

@Path("/rate-limits")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Tags({
        @Tag(name = "Rate Limits", description = "Operations related to rate limiting")
})
public class RateLimitsResource {

    private final RateLimitsPort rateLimitsPort;

    @Inject
    public RateLimitsResource(RateLimitsPort rateLimitsPort) {
        this.rateLimitsPort = rateLimitsPort;
    }

    @GET
    @APIResponse(
            responseCode = "200",
            description = "Successfully retrieved rate limits",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = RateLimitsResponse.class))
    )
    public Uni<Response> getRateLimits() {
        return rateLimitsPort.getRateLimits()
                .map(list -> Response.
                        ok(Map.of(
                                "rateLimits", list,
                                "timestamp", System.currentTimeMillis()))
                        .build());
    }
}
