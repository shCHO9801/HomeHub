package io.github.shhome.homehub.monitoring.service;

import io.github.shhome.homehub.monitoring.dto.ResourceUsageResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoProvider;

import java.time.OffsetDateTime;

import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.UNKNOWN;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final SystemInfoProvider systemInfoProvider;

    public ServerStatusResponse getServerStatus() {
        try {
            HardwareAbstractionLayer hardware =
                    systemInfoProvider.getHardware();

            OperatingSystem operatingSystem =
                    systemInfoProvider.getOperatingSystem();

            CentralProcessor processor =
                    hardware.getProcessor();

            GlobalMemory memory =
                    hardware.getMemory();

            double cpuUsagePercent = toPercent(
                    processor.getSystemCpuLoad(1000L)
            );

            ResourceUsageResponse memoryUsage = toResourceUsage(
                    memory.getTotal(),
                    memory.getAvailable()
            );

            OSFileStore rootStore = operatingSystem
                    .getFileSystem()
                    .getFileStores()
                    .stream()
                    .filter(store -> "/".equals(store.getMount()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "루트 파일시스템을 찾을 수 없습니다."
                            )
                    );

            ResourceUsageResponse diskUsage = toResourceUsage(
                    rootStore.getTotalSpace(),
                    rootStore.getUsableSpace()
            );

            return new ServerStatusResponse(
                    ONLINE,
                    cpuUsagePercent,
                    processor.getLogicalProcessorCount(),
                    memoryUsage,
                    diskUsage,
                    operatingSystem.getSystemUptime(),
                    OffsetDateTime.now()
            );

        } catch (RuntimeException e) {
            log.warn(
                    "서버 모니터링 정보를 수집하는데 실패했습니다.",
                    e
            );

            return new ServerStatusResponse(
                    UNKNOWN,
                    null,
                    null,
                    null,
                    null,
                    null,
                    OffsetDateTime.now()
            );
        }
    }

    private ResourceUsageResponse toResourceUsage(
            long totalBytes,
            long availableBytes
    ) {
        if (totalBytes <= 0 || availableBytes < 0) {
            throw new IllegalStateException(
                    "올바르지 않은 자원 사용량입니다."
            );
        }

        long normalizedAvailable = Math.clamp(
                availableBytes,
                0,
                totalBytes
        );

        long usedBytes = totalBytes - normalizedAvailable;

        double usagePercent = round(
                (double) usedBytes / totalBytes * 100
        );

        return new ResourceUsageResponse(
                totalBytes,
                usedBytes,
                normalizedAvailable,
                usagePercent
        );
    }

    private double toPercent(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0) {
            throw new IllegalStateException(
                    "올바르지 않은 CPU 사용률입니다."
            );
        }

        double percent = Math.clamp(
                ratio * 100,
                0,
                100
        );

        return round(percent);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
