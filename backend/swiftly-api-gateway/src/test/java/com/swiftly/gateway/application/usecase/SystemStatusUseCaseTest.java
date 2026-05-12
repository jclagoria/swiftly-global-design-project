package com.swiftly.gateway.application.usecase;

import com.swiftly.gateway.application.usecase.fixtures.TestFixtures;
import com.swiftly.gateway.domain.model.RegistryStatus;
import com.swiftly.gateway.domain.model.SystemStatus;
import com.swiftly.gateway.domain.port.outbound.ServiceDiscoveryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SystemStatusUseCaseTest {

    @Mock
    private ServiceDiscoveryPort serviceDiscoveryPort;

    private SimpleMeterRegistry meterRegistry;
    private SystemStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        // In-memory registry keeps tests fast and isolated
        meterRegistry = new SimpleMeterRegistry();
        useCase = new SystemStatusUseCase(serviceDiscoveryPort, meterRegistry);
    }

    /** Supplies empty and non-empty service lists to verify healthy logic. */
    static Stream<List<String>> healthyServiceLists() {
        return Stream.of(
                TestFixtures.sampleServices(0),
                TestFixtures.sampleServices(3)
        );
    }

    @ParameterizedTest
    @MethodSource("healthyServiceLists")
    @DisplayName("getSystemStatus should return healthy and update metrics")
    void getSystemStatus_shouldReturnHealthyAndUpdateMetrics(List<String> services) {
        // Given: service discovery will succeed with our fixture
        given(serviceDiscoveryPort.getRegisteredServices())
                .willReturn(Uni.createFrom().item(services));

        // When
        SystemStatus result = useCase.getSystemStatus().await().indefinitely();

        // Then: DTO assertions
        assertThat(result.getGateway()).isEqualTo("UP");
        assertThat(result.getServicesCount()).isEqualTo(services.size());
        RegistryStatus reg = result.getRegistry();
        assertThat(reg.getStatus()).isEqualTo("UP");
        assertThat(reg.getDetails()).containsEntry("services", services.size());

        // Then: metrics assertions
        double healthGauge = meterRegistry.get("system.registry.health").gauge().value();
        double countGauge  = meterRegistry.get("system.services").gauge().value();
        assertThat(healthGauge).isEqualTo(1.0);
        assertThat(countGauge).isEqualTo(services.size());
    }

    @Test
    @DisplayName("getSystemStatus should return degraded on failure and increment failure counter")
    void getSystemStatus_shouldReturnDegradedOnFailureAndIncrementFailureCounter() {
        // Given: service discovery will fail with our sample exception
        RuntimeException ex = TestFixtures.sampleException();
        given(serviceDiscoveryPort.getRegisteredServices())
                .willReturn(Uni.createFrom().failure(ex));

        // When
        SystemStatus result = useCase.getSystemStatus().await().indefinitely();

        // Then: DTO assertions
        assertThat(result.getGateway()).isEqualTo("DOWN");
        assertThat(result.getServicesCount()).isEqualTo(0);
        RegistryStatus reg = result.getRegistry();
        assertThat(reg.getStatus()).isEqualTo("DOWN");
        assertThat(reg.getDetails()).containsEntry("error", ex.getMessage());

        // Then: metrics side‐effects
        double healthGauge = meterRegistry.get("system.registry.health").gauge().value();
        double countGauge  = meterRegistry.get("system.services").gauge().value();
        Counter failure    = meterRegistry.get("system.status.failure").counter();

        assertThat(healthGauge).isEqualTo(0.0);
        assertThat(countGauge).isEqualTo(0.0);
        assertThat(failure.count()).isEqualTo(1.0);
    }

}