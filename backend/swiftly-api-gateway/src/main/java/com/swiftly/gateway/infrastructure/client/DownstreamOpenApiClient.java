package com.swiftly.gateway.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "downstream-openapi-client")
@Path("/openapi")
@Produces(MediaType.APPLICATION_JSON)
public interface DownstreamOpenApiClient {

    @Retry(maxRetries = 3)
    @Timeout(500)
    @Fallback(fallbackMethod = "emptySpec")
    @GET
    Uni<JsonNode> getOpenApi();

}
