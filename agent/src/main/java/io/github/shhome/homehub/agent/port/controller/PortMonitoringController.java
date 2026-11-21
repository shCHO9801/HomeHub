package io.github.shhome.homehub.agent.port.controller;

import io.github.shhome.homehub.agent.port.dto.PortStatusResponse;
import io.github.shhome.homehub.agent.port.service.PortMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/ports")
@RequiredArgsConstructor
public class PortMonitoringController {

    private final PortMonitoringService portMonitoringService;

    @GetMapping
    public ResponseEntity<List<PortStatusResponse>> getPorts() {
        return ResponseEntity.ok(
                portMonitoringService.getPorts()
        );
    }
}