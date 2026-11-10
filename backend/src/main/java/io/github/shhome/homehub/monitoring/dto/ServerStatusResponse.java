package io.github.shhome.homehub.monitoring.dto;

import java.time.OffsetDateTime;

public record ServerStatusResponse (
        ServerStatus status,
        Double cpuUsagePercent,
        Integer logicalProcessorCount,
        ResourceUsageResponse memory,
        ResourceUsageResponse disk,
        Long uptimeSeconds,
        OffsetDateTime collectedAt
){
}
