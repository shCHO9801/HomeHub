package io.github.shhome.homehub.monitoring.dto;

public record PortStatusResponse(
        PortProtocol protocol,
        int port,
        String bindAddress,
        PortExposure exposure,
        String processName
) {
}