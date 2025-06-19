package com.swiftly.gateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class TestMeterRegistryProducer {

    @Produces
    public MeterRegistry meterRegistry() {
        // Simple, in-memory registry that satisfies your UseCase’s Gauge/Counter needs
        return new SimpleMeterRegistry();
    }

}
