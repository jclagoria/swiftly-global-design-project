package com.swiftly.gateway.infrastructure.adapter.inbound;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import com.swiftly.gateway.domain.port.inbound.RateLimitsPort;
import com.swiftly.gateway.utils.DisableMetricsProfile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(DisableMetricsProfile.class)
class RateLimitsResourceTest {

    @InjectMock
    RateLimitsPort rateLimitsPort;

    @ParameterizedTest(name = "[{index}] GET /rate-limits → {0}.size={1}")
    @MethodSource("com.swiftly.gateway.fixtures.TestFixturesAdapter#listsProvider")
    @DisplayName("getRateLimits → 200 + correct JSON payload for various lists")
    void testGetRateLimits_success(List<RateLimitInfo> mockedList) {
        when(rateLimitsPort.getRateLimits())
                .thenReturn(Uni.createFrom().item(mockedList));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get("/rate-limits")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                // check array size
                .body("rateLimits.size()", is(mockedList.size()))
                // if non-empty, verify first element mapping
                .body("rateLimits[0].route",
                        mockedList.isEmpty() ? nullValue() : equalTo(mockedList.get(0).getRoute()))
                // verify timestamp exists and is a number
                .body("timestamp", allOf(notNullValue(), instanceOf(Number.class)));
    }

    @Test
    @DisplayName("getRateLimits → 500 when port fails")
    void testGetRateLimits_failure() {
        when(rateLimitsPort.getRateLimits())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("kaboom")));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .when()
                .get("/rate-limits")
                .then()
                .statusCode(500);
    }
}