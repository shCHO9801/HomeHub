package io.github.shhome.homehub.synology.dto;

public record SynologyVolumeResponse(
        String name,
        String path,
        long totalBytes,
        long usedBytes,
        long availableBytes,
        double usagePercent
) {
}