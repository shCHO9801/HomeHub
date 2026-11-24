package io.github.shhome.homehub.monitoring.history.repository;

import io.github.shhome.homehub.monitoring.history.entity.MonitoringSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MonitoringSnapshotRepository
        extends JpaRepository<MonitoringSnapshot, Long> {

    List<MonitoringSnapshot>
    findAllByCollectedAtBetweenOrderByCollectedAtAsc(
            LocalDateTime start,
            LocalDateTime end
    );

    long deleteByCollectedAtBefore(
            LocalDateTime threshold
    );
}
