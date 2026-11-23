package io.github.shhome.homehub.synology.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SynologyStorageResponse(
        SynologyConnectionStatus status,
        boolean stale,
        List<SynologyVolumeResponse> volumes,
        List<SynologyStorageItemResponse> storageItems,
        OffsetDateTime collectedAt
) {
}