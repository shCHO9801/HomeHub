package io.github.shhome.homehub.monitoring.alert.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MonitoringAlertSummaryResponse(
        MonitoringAlertStatus status,
        List<MonitoringAlertResponse> alerts,
        OffsetDateTime checkedAt
) {
}
