package com.swiftly.gateway.application.service;

import com.swiftly.gateway.domain.model.RouteMetadata;
import com.swiftly.gateway.domain.port.outbound.RouteRegistryAdapter;
import com.swiftly.gateway.fixtures.TestFixturesAdapter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    RouteRegistryAdapter registryAdapter;

    RouteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RouteServiceImpl(registryAdapter);
    }

    @Test
    void givenEmptyList_whenGetActiveRoutes_thenEmpty() {
        when(registryAdapter.findAllRoutes())
                .thenReturn(Uni.createFrom().item(List.<RouteMetadata>of()));

        List<RouteMetadata> result = service.getActiveRoutes()
                .await().indefinitely();

        assertThat(result).isEmpty();
    }

    @Test
    void givenAdapterFailure_whenGetActiveRoutes_thenPropagate() {
        RuntimeException ex = new RuntimeException("registry down");
        when(registryAdapter.findAllRoutes())
                .thenReturn(Uni.createFrom().failure(ex));

        UniAssertSubscriber<List<RouteMetadata>> sub =
                service.getActiveRoutes()
                        .subscribe().withSubscriber(UniAssertSubscriber.create());

        sub.awaitFailure()
                .assertFailedWith(RuntimeException.class, "registry down");
    }

    @ParameterizedTest
    @MethodSource("statusScenarios")
    void filteringStatusesParameterized(
            List<String> statuses,
            List<String> expectedIds) {

        // arrange
        List<RouteMetadata> input = TestFixturesAdapter.routes(
                statuses.toArray(new String[0])
        );
        when(registryAdapter.findAllRoutes())
                .thenReturn(Uni.createFrom().item(input));

        // act
        List<RouteMetadata> active = service.getActiveRoutes()
                .await().indefinitely();

        // assert
        assertThat(active)
                .extracting(RouteMetadata::getRouteId)
                .containsExactlyElementsOf(expectedIds);
    }

    static Stream<Arguments> statusScenarios() {
        return Stream.of(
                // empty
                Arguments.of(List.<String>of(), List.<String>of()),
                // all active
                Arguments.of(List.of("active","ACTIVE"), List.of("route0","route1")),
                // none active
                Arguments.of(List.of("inactive","disabled"), List.of()),
                // mixed & case-insensitive
                Arguments.of(List.of("Active","inactive","ACTIVE"),
                        List.of("route0","route2"))
        );
    }

}