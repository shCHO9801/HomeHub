package io.github.shhome.homehub.agent.monitoring.dto;

public record ResourceUsageResponse(
        long totalBytes,
        long usedBytes,
        long availableBytes,
        double usagePercent
) {
}
