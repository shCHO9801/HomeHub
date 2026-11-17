package io.github.shhome.homehub.agent.monitoring.controller;

import io.github.shhome.homehub.agent.monitoring.dto.AgentStatusResponse;
import io.github.shhome.homehub.agent.monitoring.service.HostMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentStatusController {

    private final HostMonitoringService hostMonitoringService;

    @GetMapping("/status")
    public ResponseEntity<AgentStatusResponse> getStatus() {
        return ResponseEntity.ok(
                hostMonitoringService.getStatus()
        );
    }
}
