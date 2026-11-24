package io.github.shhome.homehub.monitoring.history.dto;

import io.github.shhome.homehub.monitoring.history.entity.MonitoringSnapshot;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record MonitoringHistoryResponse(
        double cpuUsagePercent,
        double memoryUsagePercent,
        double diskUsagePercent,
        OffsetDateTime collectedAt
) {

    public static MonitoringHistoryResponse from(
            MonitoringSnapshot snapshot
    ) {
        return new MonitoringHistoryResponse(
                snapshot.getCpuUsagePercent().doubleValue(),
                snapshot.getMemoryUsagePercent().doubleValue(),
                snapshot.getDiskUsagePercent().doubleValue(),
                snapshot.getCollectedAt()
                        .atOffset(ZoneOffset.UTC)
        );
    }
}