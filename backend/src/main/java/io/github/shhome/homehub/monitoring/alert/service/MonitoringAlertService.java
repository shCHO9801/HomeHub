package io.github.shhome.homehub.monitoring.alert.service;

import io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertResponse;
import io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertStatus;
import io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertSummaryResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import io.github.shhome.homehub.monitoring.history.dto.MonitoringHistoryResponse;
import io.github.shhome.homehub.monitoring.history.service.MonitoringHistoryService;
import io.github.shhome.homehub.monitoring.service.MonitoringService;
import io.github.shhome.homehub.synology.dto.SynologyStorageHealth;
import io.github.shhome.homehub.synology.service.SynologyStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertSource.*;
import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertStatus.*;
import static io.github.shhome.homehub.monitoring.dto.DockerContainerState.UNHEALTHY;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;

@Service
@RequiredArgsConstructor
public class MonitoringAlertService {

    private static final double CPU_WARNING_PERCENT = 85;
    private static final double MEMORY_WARNING_PERCENT = 90;
    private static final double DISK_WARNING_PERCENT = 85;
    private static final int AVERAGE_MINUTES = 15;

    private final MonitoringHistoryService monitoringHistoryService;
    private final MonitoringService monitoringService;
    private final Clock monitoringClock;
    private final SynologyStorageService synologyStorageService;

    public MonitoringAlertSummaryResponse getAlerts() {
        OffsetDateTime checkedAt =
                OffsetDateTime.now(monitoringClock);

        List<MonitoringAlertResponse> alerts =
                new ArrayList<>();

        addServerAlerts(alerts, checkedAt);
        addDockerAlerts(alerts);
        addNasAlerts(alerts);

        return new MonitoringAlertSummaryResponse(
                resolveStatus(alerts),
                List.copyOf(alerts),
                checkedAt
        );
    }

    private void addServerAlerts(
            List<MonitoringAlertResponse> alerts,
            OffsetDateTime checkedAt
    ) {
        List<MonitoringHistoryResponse> recentHistory =
                monitoringHistoryService
                        .getRecentHistory(1)
                        .stream()
                        .filter(history ->
                                !history.collectedAt().isBefore(
                                        checkedAt.minusMinutes(
                                                AVERAGE_MINUTES
                                        )
                                )
                        )
                        .toList();

        if (recentHistory.isEmpty()) {
            alerts.add(warning(
                    "서버 이력 확인 불가",
                    "최근 15분 동안 수집된 서버 이력이 없습니다."
            ));
        } else {
            addAverageUsageAlerts(
                    alerts,
                    recentHistory
            );
        }

        ServerStatusResponse serverStatus =
                monitoringService.getServerStatus();

        if (serverStatus.status() != ONLINE
                || serverStatus.disk() == null) {
            alerts.add(warning(
                    "서버 상태 확인 불가",
                    "현재 서버 자원 상태를 확인할 수 없습니다."
            ));
            return;
        }

        double diskUsage =
                serverStatus.disk().usagePercent();

        if (diskUsage >= DISK_WARNING_PERCENT) {
            alerts.add(warning(
                    "디스크 사용률 경고",
                    formatUsageMessage(
                            "디스크",
                            diskUsage,
                            DISK_WARNING_PERCENT
                    )
            ));
        }
    }

    private void addDockerAlerts(
            List<MonitoringAlertResponse> alerts
    ) {
        try {
            monitoringService
                    .getDockerContainers()
                    .stream()
                    .filter(container ->
                            container.state() == UNHEALTHY
                    )
                    .map(container ->
                            new MonitoringAlertResponse(
                                    DOCKER,
                                    CRITICAL,
                                    "Docker 컨테이너 비정상",
                                    container.name()
                                            + " 컨테이너가 비정상 상태입니다."
                            )
                    )
                    .forEach(alerts::add);
        } catch (RuntimeException exception) {
            alerts.add(new MonitoringAlertResponse(
                    DOCKER,
                    WARNING,
                    "Docker 상태 확인 불가",
                    "Docker 컨테이너 상태를 확인할 수 없습니다."
            ));
        }
    }

    private void addNasAlerts(
            List<MonitoringAlertResponse> alerts
    ) {
        try {
            var storageStatus =
                    synologyStorageService.getStorageStatus();

            if (storageStatus.status() != AVAILABLE) {
                alerts.add(new MonitoringAlertResponse(
                        NAS,
                        WARNING,
                        "NAS 상태 확인 불가",
                        "NAS 저장소 상태를 확인할 수 없습니다."
                ));
            }

            for (var storageItem :
                    storageStatus.storageItems()) {
                if (storageItem.health()
                        == SynologyStorageHealth.WARNING) {
                    alerts.add(new MonitoringAlertResponse(
                            NAS,
                            WARNING,
                            "NAS 저장소 주의",
                            storageItem.name()
                                    + " 상태를 확인해야 합니다."
                    ));
                }

                if (storageItem.health()
                        == SynologyStorageHealth.CRITICAL) {
                    alerts.add(new MonitoringAlertResponse(
                            NAS,
                            CRITICAL,
                            "NAS 저장소 위험",
                            storageItem.name()
                                    + " 상태가 위험합니다."
                    ));
                }
            }
        } catch (RuntimeException exception) {
            alerts.add(new MonitoringAlertResponse(
                    NAS,
                    WARNING,
                    "NAS 상태 확인 불가",
                    "NAS 저장소 상태를 확인할 수 없습니다."
            ));
        }
    }

    private void addAverageUsageAlerts(
            List<MonitoringAlertResponse> alerts,
            List<MonitoringHistoryResponse> history
    ) {
        double averageCpu = history
                .stream()
                .mapToDouble(
                        MonitoringHistoryResponse::cpuUsagePercent
                )
                .average()
                .orElse(0);

        double averageMemory = history
                .stream()
                .mapToDouble(
                        MonitoringHistoryResponse::memoryUsagePercent
                )
                .average()
                .orElse(0);

        if (averageCpu >= CPU_WARNING_PERCENT) {
            alerts.add(warning(
                    "CPU 사용률 경고",
                    formatUsageMessage(
                            "최근 15분 평균 CPU",
                            averageCpu,
                            CPU_WARNING_PERCENT
                    )
            ));
        }

        if (averageMemory >= MEMORY_WARNING_PERCENT) {
            alerts.add(warning(
                    "메모리 사용률 경고",
                    formatUsageMessage(
                            "최근 15분 평균 메모리",
                            averageMemory,
                            MEMORY_WARNING_PERCENT
                    )
            ));
        }
    }

    private MonitoringAlertResponse warning(
            String title,
            String message
    ) {
        return new MonitoringAlertResponse(
                SERVER,
                WARNING,
                title,
                message
        );
    }

    private String formatUsageMessage(
            String resource,
            double usagePercent,
            double threshold
    ) {
        return String.format(
                Locale.ROOT,
                "%s 사용률이 %.1f%%로 경고 기준 %.0f%% 이상입니다.",
                resource,
                usagePercent,
                threshold
        );
    }

    private MonitoringAlertStatus resolveStatus(
            List<MonitoringAlertResponse> alerts
    ) {
        if (alerts.stream().anyMatch(
                alert -> alert.status() == CRITICAL
        )) {
            return CRITICAL;
        }

        if (!alerts.isEmpty()) {
            return WARNING;
        }

        return NORMAL;
    }
}
