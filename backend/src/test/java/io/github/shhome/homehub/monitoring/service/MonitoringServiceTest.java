package io.github.shhome.homehub.monitoring.service;

import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import oshi.spi.SystemInfoProvider;

import static io.github.shhome.homehub.monitoring.dto.ServerStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("서버 모니터링 서비스")
class MonitoringServiceTest {

    @Test
    @DisplayName("시스템 정보 수집 실패 시 UNKNOWN을 반환한다")
    void returnsUnknownWhenCollectionFails() {
        SystemInfoProvider systemInfoProvider =
                mock(SystemInfoProvider.class);

        when(systemInfoProvider.getHardware())
                .thenThrow(
                        new IllegalStateException(
                                "시스템 정보 수집 실패"
                        )
                );

        MonitoringService monitoringService =
                new MonitoringService(systemInfoProvider);

        ServerStatusResponse response =
                monitoringService.getServerStatus();

        assertEquals(UNKNOWN, response.status());
        assertNull(response.cpuUsagePercent());
        assertNull(response.logicalProcessorCount());
        assertNull(response.memory());
        assertNull(response.disk());
        assertNull(response.uptimeSeconds());
        assertNotNull(response.collectedAt());
    }
}