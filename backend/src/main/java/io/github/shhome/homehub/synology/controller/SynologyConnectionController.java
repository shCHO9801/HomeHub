package io.github.shhome.homehub.synology.controller;

import io.github.shhome.homehub.synology.dto.SynologyConnectionResponse;
import io.github.shhome.homehub.synology.service.SynologyConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/synology")
@RequiredArgsConstructor
public class SynologyConnectionController {

    private final SynologyConnectionService synologyConnectionService;

    @GetMapping("/connection")
    public ResponseEntity<SynologyConnectionResponse>
    getConnectionStatus() {
        return ResponseEntity.ok(
                synologyConnectionService
                        .getConnectionStatus()
        );
    }
}