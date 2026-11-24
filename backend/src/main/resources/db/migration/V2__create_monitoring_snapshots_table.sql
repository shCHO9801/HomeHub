CREATE TABLE monitoring_snapshots
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    cpu_usage_percent    DECIMAL(5,2) NOT NULL,
    memory_usage_percent DECIMAL(5,2) NOT NULL,
    disk_usage_percent   DECIMAL(5,2) NOT NULL,
    collected_at         DATETIME(6)  NOT NULL,

    CONSTRAINT pk_monitoring_snapshots PRIMARY KEY (id),
    CONSTRAINT chk_monitoring_snapshots_cpu
        CHECK (cpu_usage_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_monitoring_snapshots_memory
        CHECK (memory_usage_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_monitoring_snapshots_disk
        CHECK (disk_usage_percent BETWEEN 0 AND 100),

    INDEX idx_monitoring_snapshots_collected_at (collected_at)
);