package com.swiftly.gateway.infrastructure.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "downstream-health-client")
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public interface DownstreamHealthClient {

    @GET
    Uni<DownstreamHealthResponse> checkHealth(String serviceName);

}
