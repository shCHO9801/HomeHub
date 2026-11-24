package io.github.shhome.homehub.monitoring.history.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "monitoring_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoringSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "cpu_usage_percent",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal cpuUsagePercent;

    @Column(
            name = "memory_usage_percent",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal memoryUsagePercent;

    @Column(
            name = "disk_usage_percent",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal diskUsagePercent;

    @Column(
            name = "collected_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime collectedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private MonitoringSnapshot(
            double cpuUsagePercent,
            double memoryUsagePercent,
            double diskUsagePercent,
            LocalDateTime collectedAt
    ) {
        this.cpuUsagePercent = toPercent(cpuUsagePercent);
        this.memoryUsagePercent = toPercent(memoryUsagePercent);
        this.diskUsagePercent = toPercent(diskUsagePercent);
        this.collectedAt = collectedAt;
    }

    public static MonitoringSnapshot of(
            double cpuUsagePercent,
            double memoryUsagePercent,
            double diskUsagePercent,
            LocalDateTime collectedAt
    ) {
        return MonitoringSnapshot.builder()
                .cpuUsagePercent(cpuUsagePercent)
                .memoryUsagePercent(memoryUsagePercent)
                .diskUsagePercent(diskUsagePercent)
                .collectedAt(collectedAt)
                .build();
    }

    private static BigDecimal toPercent(double value) {
        return BigDecimal
                .valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}