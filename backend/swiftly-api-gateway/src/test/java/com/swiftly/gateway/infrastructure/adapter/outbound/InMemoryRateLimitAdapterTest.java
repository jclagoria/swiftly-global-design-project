package com.swiftly.gateway.infrastructure.adapter.outbound;

import com.swiftly.gateway.domain.model.RateLimitInfo;
import com.swiftly.gateway.fixtures.TestFixturesAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryRateLimitAdapterTest {

    private InMemoryRateLimitAdapter adapter;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryRateLimitAdapter();
        adapter.defaultLimit = 1000;
        adapter.defaultWindowSecs = 60;
        meterRegistry = mock(MeterRegistry.class);
        adapter.meterRegistry = meterRegistry;
    }

    @Test
    void testListRateLimits_withDefaults() {
        // uses built-in defaults (1000, 60)
        adapter.init();

        List<RateLimitInfo> infos = adapter
                .listRateLimits()
                .await()
                .indefinitely();

        assertThat(infos)
                .hasSize(TestFixturesAdapter.ENDPOINTS.size())
                .allMatch(info -> info.getLimit() == adapter.defaultLimit)
                .allMatch(info -> info.getRemaining() == adapter.defaultLimit)
                .allMatch(info -> info.getWindowSecs() == adapter.defaultWindowSecs);
    }

    @Test
    void testListRateLimits_failsWhenMeterRegistryThrows() {
        adapter.defaultLimit = 3;
        adapter.defaultWindowSecs = 2;
        adapter.init();
        doThrow(new RuntimeException("gauge-boom"))
                .when(meterRegistry)
                .gauge(any(), any(), any(), any());

        UniAssertSubscriber<List<RateLimitInfo>> subscriber = adapter.listRateLimits()
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure()
                .assertFailedWith(RuntimeException.class, "gauge-boom");
    }
}