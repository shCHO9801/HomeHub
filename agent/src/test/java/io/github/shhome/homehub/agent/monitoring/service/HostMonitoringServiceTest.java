package io.github.shhome.homehub.agent.monitoring.service;

import io.github.shhome.homehub.agent.monitoring.dto.AgentStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.spi.SystemInfoProvider;

import java.util.List;

import static io.github.shhome.homehub.agent.monitoring.dto.AgentStatus.ONLINE;
import static io.github.shhome.homehub.agent.monitoring.dto.AgentStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("호스트 모니터링 서비스")
class HostMonitoringServiceTest {

    @Test
    @DisplayName("호스트 자원 사용량을 수집한다")
    void collectsHostResourceUsage() {
        SystemInfoProvider provider = mock(SystemInfoProvider.class);
        HardwareAbstractionLayer hardware =
                mock(HardwareAbstractionLayer.class);
        OperatingSystem operatingSystem =
                mock(OperatingSystem.class);
        CentralProcessor processor = mock(CentralProcessor.class);
        GlobalMemory memory = mock(GlobalMemory.class);
        FileSystem fileSystem = mock(FileSystem.class);
        OSFileStore rootStore = mock(OSFileStore.class);

        when(provider.getHardware()).thenReturn(hardware);
        when(provider.getOperatingSystem()).thenReturn(operatingSystem);
        when(hardware.getProcessor()).thenReturn(processor);
        when(hardware.getMemory()).thenReturn(memory);
        when(processor.getSystemCpuLoad(1000L)).thenReturn(0.123456);
        when(processor.getLogicalProcessorCount()).thenReturn(8);
        when(memory.getTotal()).thenReturn(1_000L);
        when(memory.getAvailable()).thenReturn(400L);
        when(operatingSystem.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.getFileStores()).thenReturn(List.of(rootStore));
        when(rootStore.getMount()).thenReturn("/");
        when(rootStore.getTotalSpace()).thenReturn(2_000L);
        when(rootStore.getUsableSpace()).thenReturn(500L);
        when(operatingSystem.getSystemUptime()).thenReturn(3_600L);

        HostMonitoringService service =
                new HostMonitoringService(provider);

        AgentStatusResponse response = service.getStatus();

        assertEquals(ONLINE, response.status());
        assertEquals(12.35, response.cpuUsagePercent());
        assertEquals(8, response.logicalProcessorCount());
        assertEquals(600L, response.memory().usedBytes());
        assertEquals(60.0, response.memory().usagePercent());
        assertEquals(1_500L, response.disk().usedBytes());
        assertEquals(75.0, response.disk().usagePercent());
        assertEquals(3_600L, response.uptimeSeconds());
        assertNotNull(response.collectedAt());
    }

    @Test
    @DisplayName("시스템 정보 수집 실패 시 UNKNOWN을 반환한다")
    void returnsUnknownWhenCollectionFails() {
        SystemInfoProvider provider = mock(SystemInfoProvider.class);

        when(provider.getHardware()).thenThrow(
                new IllegalStateException("수집 실패")
        );

        HostMonitoringService service =
                new HostMonitoringService(provider);

        AgentStatusResponse response = service.getStatus();

        assertEquals(UNKNOWN, response.status());
        assertNull(response.cpuUsagePercent());
        assertNull(response.logicalProcessorCount());
        assertNull(response.memory());
        assertNull(response.disk());
        assertNull(response.uptimeSeconds());
        assertNotNull(response.collectedAt());
    }
}
