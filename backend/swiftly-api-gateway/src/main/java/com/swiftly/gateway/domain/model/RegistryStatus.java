package com.swiftly.gateway.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class RegistryStatus {

    private final String status;
    private final Map<String, Object> details;
}
