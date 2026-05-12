package com.swiftly.gateway.application.usecase;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import com.swiftly.gateway.domain.port.outbound.RateLimitStorePort;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import static com.swiftly.gateway.application.usecase.fixtures.TestFixtures.rateLimitsOfSize;
import static com.swiftly.gateway.application.usecase.fixtures.TestFixtures.sampleRateLimits;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RateLimitsUseCaseTest {

    @Mock
    private RateLimitStorePort storePort;

    @Mock
    private MeterRegistry meterRegistry;

    private RateLimitsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RateLimitsUseCase(storePort, meterRegistry);
    }

    @Nested
    @DisplayName("getRateLimits() – success scenarios")
    class SuccessScenarios {

        static Stream<List<RateLimitInfo>> listsProvider() {
            return Stream.of(
                    sampleRateLimits(),
                    Collections.emptyList(),
                    rateLimitsOfSize(3)
            );
        }

        @ParameterizedTest(name = "when store returns {0} entries, should propagate list and gauge count")
        @MethodSource("listsProvider")
        void testGetRateLimits_success(List<RateLimitInfo> fetched) {
            // arrange
            when(storePort.listRateLimits())
                    .thenReturn(io.smallrye.mutiny.Uni.createFrom().item(fetched));

            // act
            UniAssertSubscriber<List<RateLimitInfo>> sub =
                    useCase.getRateLimits()
                            .subscribe().withSubscriber(UniAssertSubscriber.create());

            // assert returned value
            sub.awaitItem();
            assertThat(sub.getItem())
                    .containsExactlyElementsOf(fetched);

            // assert metrics recorded, but don't compare the lambda instance itself
            verify(meterRegistry)
                    .gauge(
                            eq("ratelimits.count"),
                            eq(fetched),
                            any(ToDoubleFunction.class)
                    );
            verifyNoMoreInteractions(meterRegistry);
        }
    }

    @Nested
    @DisplayName("getRateLimits() – failure scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("when store fails, should propagate exception and not record metrics")
        void testGetRateLimits_failure() {
            // arrange
            RuntimeException boom = new RuntimeException("DB timeout");
            when(storePort.listRateLimits())
                    .thenReturn(Uni.createFrom().failure(boom));

            // act
            UniAssertSubscriber<List<RateLimitInfo>> sub =
                    useCase.getRateLimits()
                            .subscribe().withSubscriber(UniAssertSubscriber.create());

            // assert failure propagated
            sub.awaitFailure()
                    .assertFailedWith(RuntimeException.class, "DB timeout");

            // no metrics should ever have been recorded
            verifyNoInteractions(meterRegistry);
        }
    }
}