package io.github.shhome.homehub.agent.docker.controller;

import io.github.shhome.homehub.agent.docker.dto.DockerContainerResponse;
import io.github.shhome.homehub.agent.docker.service.DockerMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/docker")
@RequiredArgsConstructor
public class DockerMonitoringController {

    private final DockerMonitoringService dockerMonitoringService;

    @GetMapping("/containers")
    public ResponseEntity<List<DockerContainerResponse>> getContainers() {
        return ResponseEntity.ok(
                dockerMonitoringService.getContainers()
        );
    }
}