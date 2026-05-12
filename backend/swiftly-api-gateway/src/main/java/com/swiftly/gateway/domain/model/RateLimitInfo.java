package com.swiftly.gateway.domain.model;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RateLimitInfo {

    private final String route;
    private final int limit;
    private final int remaining;
    private final long windowSecs;

}
