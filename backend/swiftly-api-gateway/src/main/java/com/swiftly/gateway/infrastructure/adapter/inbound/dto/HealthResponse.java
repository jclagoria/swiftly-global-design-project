package com.swiftly.gateway.infrastructure.adapter.inbound.dto;

import com.swiftly.gateway.domain.model.HealthStatus;

import java.util.List;

public record HealthResponse(String status, List<HealthStatus> checks) {
}
