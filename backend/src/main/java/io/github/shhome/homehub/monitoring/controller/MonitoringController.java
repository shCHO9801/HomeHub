package io.github.shhome.homehub.monitoring.controller;

import io.github.shhome.homehub.monitoring.dto.DockerContainerResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import io.github.shhome.homehub.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/server")
    public ResponseEntity<ServerStatusResponse> getServerStatus() {
        return ResponseEntity
                .ok(monitoringService.getServerStatus());
    }

    @GetMapping("/docker/containers")
    public ResponseEntity<List<DockerContainerResponse>> getDockerContainers() {
        return ResponseEntity
                .ok(monitoringService.getDockerContainers());
    }
}
