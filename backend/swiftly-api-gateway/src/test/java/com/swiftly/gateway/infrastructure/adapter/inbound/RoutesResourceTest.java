package com.swiftly.gateway.infrastructure.adapter.inbound;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import com.swiftly.gateway.domain.model.RouteMetadata;
import com.swiftly.gateway.domain.port.inbound.RoutePort;
import com.swiftly.gateway.infrastructure.adapter.ResourceFixtures;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.List;
import java.util.stream.Stream;
@QuarkusTest
class RoutesResourceTest {

    @InjectMock
    RoutePort routePort;

    @ParameterizedTest
    @MethodSource("counts")
    void testListRoutes_variousCounts(int count) {
        List<RouteMetadata> fixtures = ResourceFixtures.routeList(count);
        when(routePort.getActiveRoutes())
                .thenReturn(Uni.createFrom().item(fixtures));

        given()
                .when().get("/routes")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$.size()", is(count))
                // if count > 0, also spot‐check first element’s fields
                .body(count > 0 ? "[0].routeId" : "ignored", count > 0 ? equalTo("route0") : anything())
                .body(count > 0 ? "[0].pathPattern" : "ignored", count > 0 ? equalTo("/route0/*") : anything())
                .body(count > 0 ? "[0].requestCount" : "ignored", count > 0 ? equalTo(42) : anything());
    }

    static Stream<Arguments> counts() {
        return Stream.of(
                Arguments.of(0),
                Arguments.of(1),
                Arguments.of(3)
        );
    }

    @Test
    void testListRoutes_failure() {
        when(routePort.getActiveRoutes())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("oops")));

        given()
                .when().get("/routes")
                .then()
                .statusCode(500)
                .body(equalTo("Failed to retrieve routes: oops"));
    }

}