package com.swiftly.gateway.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HealthStatus {

    private String service;
    private String status;
    private Map<String, Object> details;

}