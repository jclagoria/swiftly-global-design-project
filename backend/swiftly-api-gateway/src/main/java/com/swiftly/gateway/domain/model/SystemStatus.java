package com.swiftly.gateway.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class SystemStatus {

    private final String gateway;
    private final RegistryStatus registry;
    private final int servicesCount;


}
