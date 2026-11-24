package io.github.shhome.homehub.monitoring.alert.service;

import io.github.shhome.homehub.monitoring.dto.DockerContainerResponse;
import io.github.shhome.homehub.monitoring.dto.ResourceUsageResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import io.github.shhome.homehub.monitoring.history.dto.MonitoringHistoryResponse;
import io.github.shhome.homehub.monitoring.history.service.MonitoringHistoryService;
import io.github.shhome.homehub.monitoring.service.MonitoringService;
import io.github.shhome.homehub.synology.dto.SynologyStorageItemResponse;
import io.github.shhome.homehub.synology.dto.SynologyStorageHealth;
import io.github.shhome.homehub.synology.dto.SynologyStorageResponse;
import io.github.shhome.homehub.synology.service.SynologyStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertStatus.CRITICAL;
import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertStatus.NORMAL;
import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertStatus.WARNING;
import static io.github.shhome.homehub.monitoring.dto.DockerContainerState.RUNNING;
import static io.github.shhome.homehub.monitoring.dto.DockerContainerState.UNHEALTHY;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.UNKNOWN;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.UNAVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyStorageHealth.HEALTHY;
import static io.github.shhome.homehub.synology.dto.SynologyStorageItemType.STORAGE_POOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("모니터링 경고 서비스")
class MonitoringAlertServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-24T08:00:00Z");

    @Mock
    private MonitoringHistoryService monitoringHistoryService;

    @Mock
    private MonitoringService monitoringService;

    @Mock
    private SynologyStorageService synologyStorageService;

    private MonitoringAlertService monitoringAlertService;

    @BeforeEach
    void setUp() {
        monitoringAlertService = new MonitoringAlertService(
                monitoringHistoryService,
                monitoringService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                synologyStorageService
        );
    }

    @Test
    @DisplayName("모든 상태가 정상이면 경고 없이 정상 상태를 반환한다")
    void returnsNormalWhenEverySourceIsHealthy() {
        stubHealthyDependencies();

        var response = monitoringAlertService.getAlerts();

        assertEquals(NORMAL, response.status());
        assertTrue(response.alerts().isEmpty());
        assertEquals(
                OffsetDateTime.parse("2026-11-24T08:00:00Z"),
                response.checkedAt()
        );
    }

    @Test
    @DisplayName("CPU 메모리 디스크 임계치를 초과하면 경고를 반환한다")
    void returnsWarningsWhenResourceThresholdsAreExceeded() {
        when(monitoringHistoryService.getRecentHistory(1))
                .thenReturn(List.of(
                        history(88, 91, 80, 10),
                        history(90, 93, 80, 5)
                ));
        when(monitoringService.getServerStatus())
                .thenReturn(serverStatus(86));
        when(monitoringService.getDockerContainers())
                .thenReturn(List.of(runningContainer()));
        when(synologyStorageService.getStorageStatus())
                .thenReturn(healthyNas());

        var response = monitoringAlertService.getAlerts();

        assertEquals(WARNING, response.status());
        assertEquals(3, response.alerts().size());
        assertEquals(
                List.of(
                        "CPU 사용률 경고",
                        "메모리 사용률 경고",
                        "디스크 사용률 경고"
                ),
                response.alerts()
                        .stream()
                        .map(alert -> alert.title())
                        .toList()
        );
    }

    @Test
    @DisplayName("Docker 또는 NAS가 위험하면 전체 상태를 위험으로 반환한다")
    void returnsCriticalWhenDockerOrNasIsCritical() {
        when(monitoringHistoryService.getRecentHistory(1))
                .thenReturn(List.of(
                        history(10, 40, 20, 5)
                ));
        when(monitoringService.getServerStatus())
                .thenReturn(serverStatus(20));
        when(monitoringService.getDockerContainers())
                .thenReturn(List.of(new DockerContainerResponse(
                        "container-id",
                        "homehub-backend",
                        "homehub-backend:latest",
                        UNHEALTHY,
                        "Up 1 hour (unhealthy)",
                        "8080/tcp",
                        "homehub"
                )));
        when(synologyStorageService.getStorageStatus())
                .thenReturn(new SynologyStorageResponse(
                        AVAILABLE,
                        false,
                        List.of(),
                        List.of(new SynologyStorageItemResponse(
                                "Storage Pool 1",
                                STORAGE_POOL,
                                SynologyStorageHealth.CRITICAL
                        )),
                        OffsetDateTime.parse(
                                "2026-11-24T07:55:00Z"
                        )
                ));

        var response = monitoringAlertService.getAlerts();

        assertEquals(CRITICAL, response.status());
        assertEquals(2, response.alerts().size());
        assertTrue(response.alerts().stream().allMatch(
                alert -> alert.status() == CRITICAL
        ));
    }

    @Test
    @DisplayName("상태 수집에 실패하면 출처별 확인 불가 경고를 반환한다")
    void returnsWarningsWhenSourcesAreUnavailable() {
        when(monitoringHistoryService.getRecentHistory(1))
                .thenReturn(List.of());
        when(monitoringService.getServerStatus())
                .thenReturn(new ServerStatusResponse(
                        UNKNOWN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        OffsetDateTime.now()
                ));
        when(monitoringService.getDockerContainers())
                .thenThrow(new IllegalStateException("unavailable"));
        when(synologyStorageService.getStorageStatus())
                .thenReturn(new SynologyStorageResponse(
                        UNAVAILABLE,
                        false,
                        List.of(),
                        List.of(),
                        null
                ));

        var response = monitoringAlertService.getAlerts();

        assertEquals(WARNING, response.status());
        assertEquals(4, response.alerts().size());
        assertTrue(response.alerts().stream().allMatch(
                alert -> alert.status() == WARNING
        ));
    }

    private void stubHealthyDependencies() {
        when(monitoringHistoryService.getRecentHistory(1))
                .thenReturn(List.of(
                        history(10, 40, 20, 5)
                ));
        when(monitoringService.getServerStatus())
                .thenReturn(serverStatus(20));
        when(monitoringService.getDockerContainers())
                .thenReturn(List.of(runningContainer()));
        when(synologyStorageService.getStorageStatus())
                .thenReturn(healthyNas());
    }

    private MonitoringHistoryResponse history(
            double cpu,
            double memory,
            double disk,
            long minutesAgo
    ) {
        return new MonitoringHistoryResponse(
                cpu,
                memory,
                disk,
                OffsetDateTime.ofInstant(
                        NOW.minusSeconds(minutesAgo * 60),
                        ZoneOffset.UTC
                )
        );
    }

    private ServerStatusResponse serverStatus(
            double diskUsage
    ) {
        return new ServerStatusResponse(
                ONLINE,
                10.0,
                20,
                new ResourceUsageResponse(
                        100,
                        40,
                        60,
                        40
                ),
                new ResourceUsageResponse(
                        100,
                        (long) diskUsage,
                        100 - (long) diskUsage,
                        diskUsage
                ),
                100L,
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
    }

    private DockerContainerResponse runningContainer() {
        return new DockerContainerResponse(
                "container-id",
                "homehub-backend",
                "homehub-backend:latest",
                RUNNING,
                "Up 1 hour",
                "8080/tcp",
                "homehub"
        );
    }

    private SynologyStorageResponse healthyNas() {
        return new SynologyStorageResponse(
                AVAILABLE,
                false,
                List.of(),
                List.of(new SynologyStorageItemResponse(
                        "Storage Pool 1",
                        STORAGE_POOL,
                        HEALTHY
                )),
                OffsetDateTime.parse(
                        "2026-11-24T07:55:00Z"
                )
        );
    }
}
