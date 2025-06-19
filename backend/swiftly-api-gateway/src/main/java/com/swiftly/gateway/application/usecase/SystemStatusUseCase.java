package com.swiftly.gateway.application.usecase;

import com.swiftly.gateway.domain.model.RegistryStatus;
import com.swiftly.gateway.domain.model.SystemStatus;
import com.swiftly.gateway.domain.port.inbound.SystemStatusPort;
import com.swiftly.gateway.domain.port.outbound.ServiceDiscoveryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aggregates gateway and registry health into a single SystemStatus DTO.
 * <p>
 * This use‐case calls out to a ServiceDiscoveryPort to fetch all registered
 * services, updates Micrometer metrics based on the outcome, and returns
 * a SystemStatus describing both gateway and registry health.
 */
@ApplicationScoped
@Slf4j
public class SystemStatusUseCase implements SystemStatusPort {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final ServiceDiscoveryPort serviceDiscoveryPort;
    private final Counter failureCounter;
    private final AtomicInteger registryHealth;
    private final AtomicInteger servicesCount;

    @Inject
    public SystemStatusUseCase(ServiceDiscoveryPort serviceDiscoveryPort, MeterRegistry meterRegistry) {
        this.serviceDiscoveryPort = serviceDiscoveryPort;

        // Counter for number of failures
        this.failureCounter = meterRegistry.counter("system.status.failure");

        // Gauges for health and services count
        this.registryHealth = new AtomicInteger(0);
        Gauge.builder("system.registry.health", registryHealth, AtomicInteger::get)
                .description("Current health status of the service registry")
                .register(meterRegistry);

        this.servicesCount = new AtomicInteger(0);
        Gauge.builder("system.services", servicesCount, AtomicInteger::get)
                .description("Number of services registered")
                .register(meterRegistry);
    }

    @Override
    public Uni<SystemStatus> getSystemStatus() {
        return serviceDiscoveryPort.getRegisteredServices()
                .onItem().transform(this::healthy)
                .onFailure().recoverWithItem(this::degraded);
    }


    /**
     * Handles the successful outcome of the service discovery.
     * <p>
     * Updates the Micrometer metrics with the number of services and the registry health.
     * <p>
     * Returns a {@link SystemStatus} with the health of the registry and the gateway.
     *
     * @param services the list of registered services
     * @return a {@link Uni} containing the {@link SystemStatus}
     */
    private SystemStatus healthy(List<?> services) {
        log.info("Successfully fetched {} registered services", services.size());

        // Update metrics
        // The registry is healthy if it returns a non-empty list of services
        registryHealth.incrementAndGet(); // 1 indicates UP
        servicesCount.set(services.size());

        RegistryStatus registryStatus = RegistryStatus.builder()
                .status(STATUS_UP)
                .details(Map.of("services", services.size()))
                .build();

        return SystemStatus.builder()
                .gateway(STATUS_UP)
                .registry(registryStatus)
                .servicesCount(services.size())
                .build();
    }

    /**
     * Handles the failure outcome of the service discovery.
     * <p>
     * Logs the error, updates the Micrometer metrics to indicate failure,
     * and returns a SystemStatus with the registry and gateway marked as down.
     *
     * @param throwable the exception that caused the failure
     * @return a SystemStatus indicating degraded state with error details
     */
    private SystemStatus degraded(Throwable throwable) {
        // Log the error encountered during the service discovery
        log.error("Failed to fetch registered services", throwable);

        // Update metrics to reflect the degraded state
        registryHealth.set(0); // 0 indicates DOWN
        servicesCount.set(0);
        failureCounter.increment();

        // Extract error message from the throwable, use default if null
        String errorMessage = throwable.getMessage() != null
                ? throwable.getMessage() : "Unknown error";

        // Build RegistryStatus indicating the registry is down with error details
        RegistryStatus registryStatus = RegistryStatus.builder()
                .status(STATUS_DOWN)
                .details(Map.of("error", errorMessage))
                .build();

        // Return a SystemStatus indicating the gateway and registry are down
        return SystemStatus.builder()
                .gateway(STATUS_DOWN)
                .registry(registryStatus)
                .servicesCount(0)
                .build();
    }

}
