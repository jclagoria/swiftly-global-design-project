package com.swiftly.gateway.infrastructure.adapter.outbound;

import com.swiftly.gateway.domain.port.outbound.ServiceDiscoveryPort;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class StorkServiceDiscoveryAdapter implements ServiceDiscoveryPort {

    @Override
    public Uni<List<String>> getRegisteredServices() {
        return Uni.createFrom().item(
                new ArrayList<>(Stork.getInstance()
                        .getServices()
                        .keySet())
        );
    }
}
