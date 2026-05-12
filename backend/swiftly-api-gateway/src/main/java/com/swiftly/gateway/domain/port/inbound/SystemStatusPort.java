package com.swiftly.gateway.domain.port.inbound;

import com.swiftly.gateway.domain.model.SystemStatus;
import io.smallrye.mutiny.Uni;

public interface SystemStatusPort {

    /**
     * Returns overall gateway + service‐registry connectivity status
     *
     * @return A Uni containing the SystemStatus.
     */
    Uni<SystemStatus> getSystemStatus();

}
