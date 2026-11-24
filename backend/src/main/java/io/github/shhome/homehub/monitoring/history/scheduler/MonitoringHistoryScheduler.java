package io.github.shhome.homehub.monitoring.history.scheduler;

import io.github.shhome.homehub.monitoring.history.service.MonitoringHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringHistoryScheduler {

    private final MonitoringHistoryService monitoringHistoryService;

    @Scheduled(
            initialDelay = 1,
            fixedDelay = 5,
            timeUnit = TimeUnit.MINUTES
    )
    public void collectSnapshot() {
        boolean collected =
                monitoringHistoryService.collectSnapshot();

        if(!collected) {
            log.warn(
                    "서버 상태 이력 수집을 건너뛰었습니다."
            );
        }
    }

    @Scheduled(
            cron = "0 0 3 * * *",
            zone = "UTC"
    )
    public void deleteExpiredSnapshots() {
        long deletedCount =
                monitoringHistoryService.deleteExpiredSnapshots();

        if (deletedCount > 0) {
            log.info(
                    "만료된 서버 상태 이력을 삭제했습니다. count={}",
                    deletedCount
            );
        }
    }
}
