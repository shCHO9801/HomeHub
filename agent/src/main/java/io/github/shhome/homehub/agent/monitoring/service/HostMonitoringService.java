package io.github.shhome.homehub.agent.monitoring.service;

import io.github.shhome.homehub.agent.monitoring.dto.AgentStatusResponse;
import io.github.shhome.homehub.agent.monitoring.dto.ResourceUsageResponse;
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

import static io.github.shhome.homehub.agent.monitoring.dto.AgentStatus.ONLINE;
import static io.github.shhome.homehub.agent.monitoring.dto.AgentStatus.UNKNOWN;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostMonitoringService {

    private final SystemInfoProvider systemInfoProvider;

    public AgentStatusResponse getStatus() {
        try {
            HardwareAbstractionLayer hardware =
                    systemInfoProvider.getHardware();

            OperatingSystem operatingSystem =
                    systemInfoProvider.getOperatingSystem();

            CentralProcessor processor =
                    hardware.getProcessor();

            GlobalMemory memory = hardware.getMemory();

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

            return new AgentStatusResponse(
                    ONLINE,
                    cpuUsagePercent,
                    processor.getLogicalProcessorCount(),
                    memoryUsage,
                    diskUsage,
                    operatingSystem.getSystemUptime(),
                    OffsetDateTime.now()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "호스트 모니터링 정보를 수집하지 못했습니다. type={}",
                    exception.getClass().getSimpleName()
            );

            return new AgentStatusResponse(
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

        return new ResourceUsageResponse(
                totalBytes,
                usedBytes,
                normalizedAvailable,
                round((double) usedBytes / totalBytes * 100)
        );
    }

    private double toPercent(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0) {
            throw new IllegalStateException(
                    "올바르지 않은 CPU 사용률입니다."
            );
        }

        return round(Math.clamp(ratio * 100, 0, 100));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
