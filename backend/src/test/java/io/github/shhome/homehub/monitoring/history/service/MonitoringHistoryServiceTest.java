package io.github.shhome.homehub.monitoring.history.service;

import io.github.shhome.homehub.monitoring.dto.ResourceUsageResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import io.github.shhome.homehub.monitoring.history.entity.MonitoringSnapshot;
import io.github.shhome.homehub.monitoring.history.repository.MonitoringSnapshotRepository;
import io.github.shhome.homehub.monitoring.service.MonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("서버 모니터링 이력 서비스")
class MonitoringHistoryServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-11-24T00:00:00Z");

    @Mock
    private MonitoringSnapshotRepository snapshotRepository;

    @Mock
    private MonitoringService monitoringService;

    private MonitoringHistoryService monitoringHistoryService;

    @BeforeEach
    void setUp() {
        monitoringHistoryService = new MonitoringHistoryService(
                snapshotRepository,
                monitoringService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("온라인 서버 상태를 시간별 스냅샷으로 저장한다")
    void savesOnlineServerStatus() {
        when(monitoringService.getServerStatus())
                .thenReturn(onlineStatus());

        boolean collected =
                monitoringHistoryService.collectSnapshot();

        ArgumentCaptor<MonitoringSnapshot> captor =
                ArgumentCaptor.forClass(
                        MonitoringSnapshot.class
                );
        verify(snapshotRepository).save(captor.capture());

        MonitoringSnapshot snapshot = captor.getValue();
        assertTrue(collected);
        assertEquals(
                new BigDecimal("12.35"),
                snapshot.getCpuUsagePercent()
        );
        assertEquals(
                new BigDecimal("45.68"),
                snapshot.getMemoryUsagePercent()
        );
        assertEquals(
                new BigDecimal("20.48"),
                snapshot.getDiskUsagePercent()
        );
        assertEquals(
                LocalDateTime.of(
                        2026, 11, 23, 14, 47, 3
                ),
                snapshot.getCollectedAt()
        );
    }

    @Test
    @DisplayName("서버 상태를 수집하지 못하면 스냅샷을 저장하지 않는다")
    void skipsUnavailableServerStatus() {
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

        boolean collected =
                monitoringHistoryService.collectSnapshot();

        assertFalse(collected);
        verifyNoInteractions(snapshotRepository);
    }

    @Test
    @DisplayName("지정한 시간 범위의 스냅샷을 오래된 순서로 조회한다")
    void getsRecentHistory() {
        LocalDateTime end =
                LocalDateTime.of(2026, 11, 24, 0, 0);
        LocalDateTime start = end.minusHours(24);
        MonitoringSnapshot snapshot = MonitoringSnapshot.of(
                12.35,
                45.68,
                20.48,
                start.plusMinutes(5)
        );

        when(snapshotRepository
                .findAllByCollectedAtBetweenOrderByCollectedAtAsc(
                        start,
                        end
                ))
                .thenReturn(List.of(snapshot));

        var history =
                monitoringHistoryService.getRecentHistory(24);

        assertEquals(1, history.size());
        assertEquals(12.35, history.getFirst().cpuUsagePercent());
        assertEquals(45.68, history.getFirst().memoryUsagePercent());
        assertEquals(20.48, history.getFirst().diskUsagePercent());
        assertEquals(
                start.plusMinutes(5).atOffset(ZoneOffset.UTC),
                history.getFirst().collectedAt()
        );
    }

    @Test
    @DisplayName("30일이 지난 스냅샷을 삭제한다")
    void deletesExpiredSnapshots() {
        LocalDateTime threshold =
                LocalDateTime.of(2026, 10, 25, 0, 0);

        when(snapshotRepository
                .deleteByCollectedAtBefore(threshold))
                .thenReturn(3L);

        long deletedCount =
                monitoringHistoryService
                        .deleteExpiredSnapshots();

        assertEquals(3L, deletedCount);
        verify(snapshotRepository)
                .deleteByCollectedAtBefore(threshold);
    }

    private ServerStatusResponse onlineStatus() {
        return new ServerStatusResponse(
                ONLINE,
                12.345,
                20,
                new ResourceUsageResponse(
                        16_000,
                        7_309,
                        8_691,
                        45.678
                ),
                new ResourceUsageResponse(
                        100_000,
                        20_480,
                        79_520,
                        20.48
                ),
                100L,
                OffsetDateTime.parse(
                        "2026-11-23T23:47:03+09:00"
                )
        );
    }
}
