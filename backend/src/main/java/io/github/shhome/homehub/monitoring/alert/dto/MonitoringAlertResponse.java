package io.github.shhome.homehub.monitoring.alert.dto;

public record MonitoringAlertResponse(
        MonitoringAlertSource source,
        MonitoringAlertStatus status,
        String title,
        String message
) {
}