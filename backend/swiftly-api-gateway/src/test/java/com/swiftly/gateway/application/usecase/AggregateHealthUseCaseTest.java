package com.swiftly.gateway.application.usecase;

import com.swiftly.gateway.domain.model.HealthStatus;
import com.swiftly.gateway.domain.port.outbound.ServiceDiscoveryPort;
import com.swiftly.gateway.fixtures.TestFixtures;
import com.swiftly.gateway.infrastructure.client.DownstreamHealthClient;
import com.swiftly.gateway.infrastructure.client.DownstreamHealthResponse;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AggregateHealthUseCaseTest {

    @Mock
    ServiceDiscoveryPort serviceDiscoveryPort;

    @Mock
    DownstreamHealthClient downstreamHealthClient;

    @InjectMocks
    AggregateHealthUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        // Inject a default timeout for Uni.ifNoItem().after()
        Field timeoutField = AggregateHealthUseCase.class.getDeclaredField("checkTimeout");
        timeoutField.setAccessible(true);
        timeoutField.set(useCase, Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Check all services happy path")
    void testCheckAllHappyPath() {
        // Arrange
        List<String> services = TestFixtures.services();
        given(serviceDiscoveryPort.getRegisteredServices()).willReturn(Uni.createFrom().item(services));
        DownstreamHealthResponse response = TestFixtures.healthyDownstreamResponse();
        services.forEach(s ->
                given(downstreamHealthClient.checkHealth(s)).willReturn(Uni.createFrom().item(response))
        );

        // Act
        List<HealthStatus> statuses = useCase.checkAll()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted()
                .getItem();

        // Assert: each service is UP with expected details
        assertThat(statuses).hasSize(services.size() + 1);
        for (int i = 0; i < services.size(); i++) {
            HealthStatus hs = statuses.get(i);
            assertThat(hs.getService()).isEqualTo(services.get(i));
            assertThat(hs.getStatus()).isEqualTo("UP");
            assertThat(hs.getDetails()).containsEntry("detailKey", "detailValue");
        }
        // Assert: gateway status at the end
        HealthStatus gateway = statuses.getLast();
        assertThat(gateway.getService()).isEqualTo("gateway");
        assertThat(gateway.getStatus()).isEqualTo("UP");
        assertThat(gateway.getDetails()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Check all with service discovery failure")
    void testCheckAllDiscoveryFailure() {
        // Arrange: service discovery fails
        given(serviceDiscoveryPort.getRegisteredServices())
                .willReturn(Uni.createFrom().failure(new RuntimeException("discovery failed")));

        // Act
        List<HealthStatus> statuses = useCase.checkAll()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted()
                .getItem();

        // Assert: only gateway status
        assertThat(statuses).hasSize(1);
        HealthStatus hs = statuses.getFirst();
        assertThat(hs.getService()).isEqualTo("gateway");
        assertThat(hs.getStatus()).isEqualTo("UP");
        assertThat(hs.getDetails()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Check all with partial failure")
    void testCheckAllPartialFailure() {
        // Arrange: one healthy, one failing service
        List<String> services = TestFixtures.services();
        given(serviceDiscoveryPort.getRegisteredServices()).willReturn(Uni.createFrom().item(services));
        given(downstreamHealthClient.checkHealth("serviceA"))
                .willReturn(Uni.createFrom().item(TestFixtures.healthyDownstreamResponse()));
        given(downstreamHealthClient.checkHealth("serviceB"))
                .willReturn(Uni.createFrom().failure(new RuntimeException("timeout")));

        // Act
        List<HealthStatus> statuses = useCase.checkAll()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted()
                .getItem();

        // Assert: healthy
        HealthStatus first = statuses.getFirst();
        assertThat(first.getService()).isEqualTo("serviceA");
        assertThat(first.getStatus()).isEqualTo("UP");
        assertThat(first.getDetails()).containsEntry("detailKey", "detailValue");
        // Assert: failing
        HealthStatus second = statuses.get(1);
        assertThat(second.getService()).isEqualTo("serviceB");
        assertThat(second.getStatus()).isEqualTo("DOWN");
        assertThat(second.getDetails()).containsKey("error").containsKey("timestamp");
        // Assert: gateway
        HealthStatus gateway = statuses.get(2);
        assertThat(gateway.getService()).isEqualTo("gateway");
        assertThat(gateway.getStatus()).isEqualTo("UP");
        assertThat(gateway.getDetails()).containsKey("timestamp");
    }

    @ParameterizedTest
    @MethodSource("servicesProvider")
    @DisplayName("Check all with various service lists")
    void testCheckAllVariousServices(List<String> services) {
        // Arrange: different service list sizes
        given(serviceDiscoveryPort.getRegisteredServices()).willReturn(Uni.createFrom().item(services));
        services.forEach(s ->
                given(downstreamHealthClient.checkHealth(s))
                        .willReturn(Uni.createFrom().item(TestFixtures.healthyDownstreamResponse()))
        );

        // Act
        List<HealthStatus> statuses = useCase.checkAll()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertCompleted()
                .getItem();

        // Assert: size = services + gateway
        assertThat(statuses).hasSize(services.size() + 1);
    }

    static Stream<List<String>> servicesProvider() {
        return Stream.of(
                List.of(),
                List.of("onlyService")
        );
    }

}