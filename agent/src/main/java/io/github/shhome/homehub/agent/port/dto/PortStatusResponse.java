package io.github.shhome.homehub.agent.port.dto;

public record PortStatusResponse(
        PortProtocol protocol,
        int port,
        String bindAddress,
        PortExposure exposure,
        String processName
) {
}