package io.github.shhome.homehub.monitoring.history.controller;

import io.github.shhome.homehub.monitoring.history.dto.MonitoringHistoryResponse;
import io.github.shhome.homehub.monitoring.history.service.MonitoringHistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring/history")
@RequiredArgsConstructor
public class MonitoringHistoryController {

    private final MonitoringHistoryService monitoringHistoryService;

    @GetMapping
    public ResponseEntity<List<MonitoringHistoryResponse>>
    getHistory(
            @RequestParam(defaultValue = "24")
            @Min(value = 1, message = "조회 시간은 1시간 이상이어야 합니다.")
            @Max(value = 720, message = "조회 시간은 720시간 이하여야 합니다.")
            int hours
    ) {
        return ResponseEntity.ok(
                monitoringHistoryService
                        .getRecentHistory(hours)
        );
    }
}
