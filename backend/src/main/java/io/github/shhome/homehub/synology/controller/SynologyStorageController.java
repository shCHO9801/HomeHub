package io.github.shhome.homehub.synology.controller;

import io.github.shhome.homehub.synology.dto.SynologyStorageResponse;
import io.github.shhome.homehub.synology.service.SynologyStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/synology")
@RequiredArgsConstructor
public class SynologyStorageController {

    private final SynologyStorageService synologyStorageService;

    @GetMapping("/storage")
    public ResponseEntity<SynologyStorageResponse> getStorageStatus() {
        return ResponseEntity.ok(
                synologyStorageService.getStorageStatus()
        );
    }
}
