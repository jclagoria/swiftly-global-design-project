package com.swiftly.gateway.application.usecase;

import com.swiftly.gateway.domain.model.HealthStatus;
import com.swiftly.gateway.domain.model.Status;
import com.swiftly.gateway.domain.port.inbound.HealthCheckPort;
import com.swiftly.gateway.domain.port.outbound.ServiceDiscoveryPort;
import com.swiftly.gateway.infrastructure.client.DownstreamHealthClient;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AggregateHealthUseCase implements HealthCheckPort {

    private static final Logger LOGGER = Logger.getLogger(AggregateHealthUseCase.class);

    @ConfigProperty(name = "health.check.timeout", defaultValue = "2S")
    Duration checkTimeout;

    private final ServiceDiscoveryPort serviceDiscoveryPort;
    @RestClient
    private final DownstreamHealthClient downstreamHealthClient;

    @Inject
    public AggregateHealthUseCase(ServiceDiscoveryPort serviceDiscoveryPort,
                                  @RestClient DownstreamHealthClient downstreamHealthClient) {
        this.serviceDiscoveryPort = serviceDiscoveryPort;
        this.downstreamHealthClient = downstreamHealthClient;
    }

    @Override
    @Counted(value = "healthChecksTotal", description = "Total number of health check requests")
    @Timed(value = "healthChecksDuration", description = "Duration of health check execution")
    public Uni<List<HealthStatus>> checkAll() {
        return serviceDiscoveryPort.getRegisteredServices()
                .onFailure().recoverWithItem(Collections.emptyList())
                .onItem().invoke(list -> LOGGER.infof("Starting health checks for %d services", list.size()))
                .onItem().transformToMulti(Multi.createFrom()::iterable)
                .onItem().transformToUniAndMerge(this::checkServiceHealth)
                .collect().asList()
                .map(list -> {
                    List<HealthStatus> combined = new ArrayList<>(list);
                    combined.add(gatewayStatus());
                    return Collections.unmodifiableList(combined);
                });
    }

    /**
     * Executes a health check for a given service.
     *
     * @param serviceName the name of the service to check
     * @return a Uni containing the health status of the service
     */
    private Uni<HealthStatus> checkServiceHealth(String serviceName) {
        return downstreamHealthClient.checkHealth(serviceName)
                .ifNoItem().after(checkTimeout).fail()
                .onItem().transform(resp -> {
                    // successful health check
                    return new HealthStatus(
                            serviceName,
                            resp.getStatus(),
                            resp.getDetails()
                    );
                })
                .onFailure().recoverWithItem(throwable -> {
                    // failed health check
                    LOGGER.errorf(throwable, "Health check failed for %s", serviceName);
                    return new HealthStatus(
                            serviceName,
                            Status.DOWN.name(),
                            Map.of(
                                    "error", throwable.getMessage(),
                                    "timestamp", Instant.now().toString()
                            )
                    );
                });
    }

    /**
     * Returns a health status indicating that the gateway is up.
     * <p>
     * This is just a placeholder, in the future this should be replaced with a real health check of the gateway.
     *
     * @return a health status indicating that the gateway is up
     */
    private HealthStatus gatewayStatus() {
        return new HealthStatus(
                "gateway",
                Status.UP.name(),
                Map.of("timestamp", Instant.now().toString())
        );
    }
}
