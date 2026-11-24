package io.github.shhome.homehub.monitoring.history.service;

import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import io.github.shhome.homehub.monitoring.history.dto.MonitoringHistoryResponse;
import io.github.shhome.homehub.monitoring.history.entity.MonitoringSnapshot;
import io.github.shhome.homehub.monitoring.history.repository.MonitoringSnapshotRepository;
import io.github.shhome.homehub.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;

@Service
@RequiredArgsConstructor
public class MonitoringHistoryService {

    private static final int RETENTION_DAYS = 30;

    private final MonitoringSnapshotRepository snapshotRepository;
    private final MonitoringService monitoringService;
    private final Clock monitoringClock;

    @Transactional
    public boolean collectSnapshot() {
        ServerStatusResponse status =
                monitoringService.getServerStatus();

        if (!canCollect(status)) {
            return false;
        }

        MonitoringSnapshot snapshot =
                MonitoringSnapshot.of(
                        status.cpuUsagePercent(),
                        status.memory().usagePercent(),
                        status.disk().usagePercent(),
                        status.collectedAt()
                                .withOffsetSameInstant(
                                        ZoneOffset.UTC
                                )
                                .toLocalDateTime()
                );

        snapshotRepository.save(snapshot);

        return true;
    }

    @Transactional(readOnly = true)
    public List<MonitoringHistoryResponse> getRecentHistory(
            int hours
    ) {
        LocalDateTime end =
                LocalDateTime.now(monitoringClock);
        LocalDateTime start =
                end.minusHours(hours);

        return snapshotRepository
                .findAllByCollectedAtBetweenOrderByCollectedAtAsc(
                        start,
                        end
                )
                .stream()
                .map(MonitoringHistoryResponse::from)
                .toList();
    }

    @Transactional
    public long deleteExpiredSnapshots() {
        LocalDateTime threshold =
                LocalDateTime
                        .now(monitoringClock)
                        .minusDays(RETENTION_DAYS);

        return snapshotRepository
                .deleteByCollectedAtBefore(threshold);
    }

    private boolean canCollect(
            ServerStatusResponse status
    ) {
        return status.status() == ONLINE
                && status.cpuUsagePercent() != null
                && status.memory() != null
                && status.disk() != null
                && status.collectedAt() != null;
    }
}