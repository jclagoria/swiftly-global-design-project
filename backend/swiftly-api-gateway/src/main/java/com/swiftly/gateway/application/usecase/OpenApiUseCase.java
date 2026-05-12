package com.swiftly.gateway.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swiftly.gateway.domain.port.inbound.OpenApiPort;
import com.swiftly.gateway.domain.port.outbound.OpenApiStorePort;
import com.swiftly.gateway.domain.port.outbound.ServiceDiscoveryPort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApplicationScoped
public class OpenApiUseCase implements OpenApiPort {

    private final ServiceDiscoveryPort serviceDiscoveryPort;
    private final OpenApiStorePort  openApiStorePort;

    @Inject
    public  OpenApiUseCase(ServiceDiscoveryPort serviceDiscoveryPort, OpenApiStorePort openApiStorePort) {
        this.serviceDiscoveryPort = serviceDiscoveryPort;
        this.openApiStorePort = openApiStorePort;
    }

    /**
     * Retrieves and merges the OpenAPI specs from all registered services.
     * <p>
     * 1. Discovers all registered service names.<br>
     * 2. Fetches each service's OpenAPI spec in parallel.<br>
     * 3. Logs and recovers from any fetch failures by using an empty object.<br>
     * 4. Merges the resulting list of specs into a single JSON document.
     *
     * @return a Uni emitting the combined OpenAPI JSON
     */
    @Override
    public Uni<JsonNode> getAggregateOpenApi() {
        return serviceDiscoveryPort.getRegisteredServices()
                .onItem().transformToMulti(Multi.createFrom()::iterable)
                .onItem().transformToUniAndMerge(serviceName -> openApiStorePort.fetchOpenApi(serviceName)
                                .onFailure().invoke(e ->
                                        log.error("Failed to fetch OpenAPI for service '{}'", serviceName, e))
                                .onFailure().recoverWithItem(JsonNodeFactory.instance.objectNode()))
                .collect().asList()
                .map(this::mergeSpecs);
    }

    /**
     * Merges multiple OpenAPI JsonNode specs into one aggregate spec.
     * Paths and component schemas are combined; conflicting keys will
     * be overwritten by later specs in the list.
     *
     * @param specs list of individual service OpenAPI specs
     * @return a single merged JsonNode spec
     */
    private JsonNode mergeSpecs(List<JsonNode> specs) {
        if (specs == null || specs.isEmpty()) {
            return JsonNodeFactory.instance.objectNode();
        }

        ObjectNode merged = specs.getFirst().deepCopy();
        ObjectNode paths = JsonNodeFactory.instance.objectNode();
        ObjectNode components = JsonNodeFactory.instance.objectNode();

        specs.forEach(spec -> {
            ObjectNode path = (ObjectNode) spec.get("path");
            paths.fieldNames()
                    .forEachRemaining(key -> path.set(key, spec.get(key)));

            if (spec.has("components") &&
                    spec.get("components").has("schemas")) {
                ObjectNode schemas = (ObjectNode)
                        spec.get("components").get("schemas");
                schemas.fieldNames().forEachRemaining(key ->
                        components.set("components", schemas.get(key)));
            }
        });

        merged.set("paths", paths);
        merged.with("components")
                .set("schemas", components.with("schemas"));
        return merged;
    }

}
