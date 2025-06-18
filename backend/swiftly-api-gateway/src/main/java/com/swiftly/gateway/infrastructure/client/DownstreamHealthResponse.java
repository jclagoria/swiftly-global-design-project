package com.swiftly.gateway.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DownstreamHealthResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("details")
    private Map<String, Object> details;



}
