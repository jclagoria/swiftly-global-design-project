package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.model.SystemStatus;
import com.swiftly.gateway.domain.port.inbound.SystemStatusPort;
import com.swiftly.gateway.fixtures.TestFixtures;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;

@QuarkusTest
class StatusResourceTest {

    @InjectMock
    SystemStatusPort systemStatusPort;

    @BeforeEach
    void setup() {
        Mockito.reset(systemStatusPort);
    }
    /**
     * Provide different service‐counts for healthy cases.
     */
    static Stream<Integer> healthyServiceCounts() {
        return Stream.of(0, 5, 10);
    }

    @ParameterizedTest
    @MethodSource("healthyServiceCounts")
    @DisplayName("status should return 200 and correct JSON for healthy")
    void status_shouldReturn200_andCorrectJson_forHealthy(int serviceCount) {
        // Given: a successful SystemStatus with N services
        SystemStatus healthy = TestFixtures.sampleSystemStatus(serviceCount);
        given(systemStatusPort.getSystemStatus())
                .willReturn(Uni.createFrom().item(healthy));

        // When & Then: GET /status → 200 + correct JSON
        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/status")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("gateway", equalTo("UP"))
                .body("servicesCount", equalTo(serviceCount))
                .body("registry.status", equalTo("UP"))
                .body("registry.details.services", equalTo(serviceCount));
    }

    @Test
    @DisplayName("status should return 503 and error JSON on failure")
    void status_shouldReturn503_andErrorJson_onFailure() {
        // Given: registry throws an exception
        RuntimeException ex = TestFixtures.sampleException();
        given(systemStatusPort.getSystemStatus())
                .willReturn(Uni.createFrom().failure(ex));

        // When & Then: GET /status → 503 + {"error":"..."}
        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/status")
                .then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("error", equalTo(ex.getMessage()));
    }

}