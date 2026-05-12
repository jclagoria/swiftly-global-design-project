package com.swiftly.gateway.infrastructure.adapter.outbound;

import com.swiftly.gateway.domain.model.RouteMetadata;
import com.swiftly.gateway.fixtures.TestFixturesAdapter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorkRouteRegistryAdapterTest {

    private StorkRouteRegistryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StorkRouteRegistryAdapter();
    }

    @ParameterizedTest
    @MethodSource("provideServiceNameLists")
    void testFindAllRoutesParameterized(List<String> serviceNames) {
        Instant now = TestFixturesAdapter.fixedInstant();

        // stub Instant.now()
        try (MockedStatic<Instant> instMock = mockStatic(Instant.class)) {
            instMock.when(Instant::now).thenReturn(now);

            // stub Stork.getInstance().getServices()
            Map<String, Service> services =
                    TestFixturesAdapter.servicesMap(serviceNames.toArray(String[]::new));
            Stork storkMock = mock(Stork.class);
            when(storkMock.getServices()).thenReturn(services);
            when(storkMock.getServices()).thenReturn(services);

            try (MockedStatic<Stork> storkStatic = mockStatic(Stork.class)) {
                storkStatic.when(Stork::getInstance).thenReturn(storkMock);

                Uni<List<RouteMetadata>> uni = adapter.findAllRoutes();
                List<RouteMetadata> result = uni.await().indefinitely();

                // build expected list
                List<RouteMetadata> expected = serviceNames.stream()
                        .map(name -> TestFixturesAdapter.routeMetadata(name, now))
                        .toList();

                assertThat(result).containsExactlyElementsOf(expected);
            }
        }
    }

    static Stream<Arguments> provideServiceNameLists() {
        return Stream.of(
                Arguments.of(List.<String>of()),
                Arguments.of(List.of("serviceA")),
                Arguments.of(List.of("serviceA", "serviceB", "serviceC"))
        );
    }

    /** If the registry throws, the Uni should propagate failure. */
    @Test
    void testFindAllRoutesFailure() {
        // stub Stork.getInstance().getServices() to throw
        Stork storkMock = mock(Stork.class);
        when(storkMock.getServices()).thenThrow(new RuntimeException("registry down"));

        try (MockedStatic<Stork> storkStatic = mockStatic(Stork.class)) {
            storkStatic.when(Stork::getInstance).thenReturn(storkMock);

            UniAssertSubscriber<List<RouteMetadata>> sub =
                    adapter.findAllRoutes().subscribe().withSubscriber(UniAssertSubscriber.create());

            sub.awaitFailure();
            assertThat(sub.getFailure())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("registry down");
        }
    }

}