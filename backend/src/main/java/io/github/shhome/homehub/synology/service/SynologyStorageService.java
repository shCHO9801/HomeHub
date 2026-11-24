package io.github.shhome.homehub.synology.service;

import io.github.shhome.homehub.synology.client.SynologySnmpClient;
import io.github.shhome.homehub.synology.client.dto.SnmpStorageItemData;
import io.github.shhome.homehub.synology.client.dto.SnmpVolumeData;
import io.github.shhome.homehub.synology.client.dto.SynologySnmpSnapshot;
import io.github.shhome.homehub.synology.config.SynologySnmpProperties;
import io.github.shhome.homehub.synology.dto.SynologyStorageHealth;
import io.github.shhome.homehub.synology.dto.SynologyStorageItemResponse;
import io.github.shhome.homehub.synology.dto.SynologyStorageItemType;
import io.github.shhome.homehub.synology.dto.SynologyStorageResponse;
import io.github.shhome.homehub.synology.dto.SynologyVolumeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.UNAVAILABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynologyStorageService {

    private final Optional<SynologySnmpClient> synologySnmpClient;
    private final SynologySnmpProperties properties;

    private SynologyStorageResponse cachedResponse;
    private SynologyStorageResponse lastSuccessfulResponse;
    private Instant refreshAfter = Instant.EPOCH;

    public synchronized SynologyStorageResponse getStorageStatus() {
        Instant now = Instant.now();

        if (cachedResponse != null
                && now.isBefore(refreshAfter)) {
            return cachedResponse;
        }

        cachedResponse = refresh();
        refreshAfter = now.plus(properties.cacheTtl());

        return cachedResponse;
    }

    private SynologyStorageResponse refresh() {
        if (synologySnmpClient.isEmpty()) {
            return unavailableResponse();
        }

        try {
            SynologySnmpSnapshot snapshot =
                    synologySnmpClient.get().getSnapshot();

            SynologyStorageResponse response =
                    availableResponse(snapshot);

            lastSuccessfulResponse = response;

            return response;
        } catch (RuntimeException exception) {
            log.warn(
                    "Synology 저장공간 수집에 실패했습니다. type={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            return unavailableResponse();
        }
    }

    private SynologyStorageResponse availableResponse(
            SynologySnmpSnapshot snapshot
    ) {
        List<SynologyVolumeResponse> volumes = snapshot
                .volumes()
                .stream()
                .map(this::toVolumeResponse)
                .toList();

        if (volumes.isEmpty()) {
            throw new IllegalStateException(
                    "Synology 볼륨 정보를 찾을 수 없습니다."
            );
        }

        List<SynologyStorageItemResponse> storageItems =
                snapshot
                        .storageItems()
                        .stream()
                        .map(this::toStorageItemResponse)
                        .toList();

        return new SynologyStorageResponse(
                AVAILABLE,
                false,
                volumes,
                storageItems,
                OffsetDateTime.now()
        );
    }

    private SynologyStorageResponse unavailableResponse() {
        if (lastSuccessfulResponse == null) {
            return new SynologyStorageResponse(
                    UNAVAILABLE,
                    false,
                    List.of(),
                    List.of(),
                    null
            );
        }

        return new SynologyStorageResponse(
                UNAVAILABLE,
                true,
                lastSuccessfulResponse.volumes(),
                lastSuccessfulResponse.storageItems(),
                lastSuccessfulResponse.collectedAt()
        );
    }

    private SynologyVolumeResponse toVolumeResponse(
            SnmpVolumeData volume
    ) {
        validateVolume(volume);

        long totalBytes = Math.multiplyExact(
                volume.allocationUnit(),
                volume.totalUnits()
        );
        long usedBytes = Math.multiplyExact(
                volume.allocationUnit(),
                volume.usedUnits()
        );
        long availableBytes = totalBytes - usedBytes;

        double usagePercent = round(
                (double) usedBytes / totalBytes * 100
        );

        String volumeNumber = volume
                .path()
                .substring("/volume".length());

        return new SynologyVolumeResponse(
                "Volume " + volumeNumber,
                volume.path(),
                totalBytes,
                usedBytes,
                availableBytes,
                usagePercent
        );
    }

    private void validateVolume(SnmpVolumeData volume) {
        if (volume.allocationUnit() <= 0
                || volume.totalUnits() <= 0
                || volume.usedUnits() < 0
                || volume.usedUnits()
                > volume.totalUnits()) {
            throw new IllegalStateException(
                    "올바르지 않은 Synology 볼륨 정보입니다."
            );
        }
    }

    private SynologyStorageItemResponse
    toStorageItemResponse(
            SnmpStorageItemData storageItem
    ) {
        return new SynologyStorageItemResponse(
                storageItem.name(),
                toStorageItemType(storageItem.name()),
                toStorageHealth(storageItem.statusCode())
        );
    }

    private SynologyStorageItemType toStorageItemType(
            String name
    ) {
        if (name.startsWith("Volume ")) {
            return SynologyStorageItemType.VOLUME;
        }

        if (name.startsWith("Storage Pool ")) {
            return SynologyStorageItemType.STORAGE_POOL;
        }

        return SynologyStorageItemType.UNKNOWN;
    }

    private SynologyStorageHealth toStorageHealth(
            int statusCode
    ) {
        return switch (statusCode) {
            case 1 -> SynologyStorageHealth.HEALTHY;
            case 11 -> SynologyStorageHealth.WARNING;
            case 12 -> SynologyStorageHealth.CRITICAL;
            default -> SynologyStorageHealth.UNKNOWN;
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}