package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.port.inbound.OpenApiPort;
import io.micrometer.core.annotation.Timed;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.opentracing.Traced;

import java.util.Map;

@Path("/openapi")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name="OpenAPI", description="Aggregated OpenAPI for all downstream services")
@Traced
public class OpenApiResource {

    private final OpenApiPort openApiUseCase;

    @Inject
    public OpenApiResource(OpenApiPort openApiUseCase) {
        this.openApiUseCase = openApiUseCase;
    }

    @GET
    @Timed(value = "openapi.resource.openapi",
            description = "Time spent serving the aggregated OpenAPI document")
    public Uni<Response> openapi() {
        return openApiUseCase.getAggregateOpenApi()
                .map(spec -> {
                    // Compute a simple ETag based on the JSON content
                    String etag = "\"" + Integer.toHexString(spec.hashCode()) + "\"";
                    return Response.ok(spec)
                            .tag(etag)  // sets the ETag header
                            .header("Cache-Control", "public, max-age=3600")
                            .build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    // Return a JSON error payload and no-cache headers
                    Map<String, String> errorBody = Map.of("error", throwable.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(errorBody)
                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                            .build();
                });
    }

}
