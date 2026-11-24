package io.github.shhome.homehub.monitoring.alert.controller;

import io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertSummaryResponse;
import io.github.shhome.homehub.monitoring.alert.service.MonitoringAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/alerts")
@RequiredArgsConstructor
public class MonitoringAlertController {

    private final MonitoringAlertService monitoringAlertService;

    @GetMapping
    public ResponseEntity<MonitoringAlertSummaryResponse>
    getAlerts() {
        return ResponseEntity.ok(
                monitoringAlertService.getAlerts()
        );
    }
}