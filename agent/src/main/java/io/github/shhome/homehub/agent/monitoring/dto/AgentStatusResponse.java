package io.github.shhome.homehub.agent.monitoring.dto;

import java.time.OffsetDateTime;

public record AgentStatusResponse(
        AgentStatus status,
        Double cpuUsagePercent,
        Integer logicalProcessorCount,
        ResourceUsageResponse memory,
        ResourceUsageResponse disk,
        Long uptimeSeconds,
        OffsetDateTime collectedAt
) {
}
