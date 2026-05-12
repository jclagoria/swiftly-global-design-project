package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.model.HealthStatus;
import com.swiftly.gateway.domain.port.inbound.HealthCheckPort;
import com.swiftly.gateway.fixtures.TestFixturesAdapter;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class HealthCheckResourceTest {

    @InjectMock
    HealthCheckPort healthCheckPort;

    @BeforeEach
    void setup() {
        Mockito.reset(healthCheckPort);
    }

    static Stream<Arguments> provideHealthScenarios() {
        return Stream.of(
                Arguments.of(
                        "All services UP",
                        List.of(TestFixturesAdapter.upStatus("svc1"), TestFixturesAdapter.upStatus("svc2")),
                        "UP"
                ),
                Arguments.of(
                        "One service DOWN",
                        List.of(TestFixturesAdapter.upStatus("svc1"), TestFixturesAdapter.downStatus("svc2")),
                        "DOWN"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideHealthScenarios")
    void testHealthEndpointHappyPath(String scenario, List<HealthStatus> mockStatuses, String expectedOverall) {
        // Stub the healthUseCase to return our mock statuses
        BDDMockito.given(healthCheckPort.checkAll())
                .willReturn(Uni.createFrom().item(mockStatuses));

        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo(expectedOverall))
                 .body("checks", hasSize(mockStatuses.size()));
    }

    @DisplayName("Health endpoint returns 500 and error payload when use case fails")
    @Test
    void testHealthEndpointError() {
        Throwable ex = new RuntimeException("downstream failure");
        BDDMockito.given(healthCheckPort.checkAll())
                .willReturn(Uni.createFrom().failure(ex));

        given()
                .when().get("/health")
                .then()
                .statusCode(500)
                .body("status", equalTo("DOWN"))
                .body("checks", hasSize(1))
                .body("checks[0].service", equalTo("health-endpoint"))
                .body("checks[0].status", equalTo("DOWN"))
                .body("checks[0].details.error", equalTo(ex.getMessage()));
    }

}