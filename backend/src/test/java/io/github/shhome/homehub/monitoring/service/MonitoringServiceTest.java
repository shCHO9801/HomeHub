package io.github.shhome.homehub.monitoring.service;

import io.github.shhome.homehub.monitoring.client.HostAgentClient;
import io.github.shhome.homehub.monitoring.dto.DockerContainerResponse;
import io.github.shhome.homehub.monitoring.dto.ResourceUsageResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;

import static io.github.shhome.homehub.monitoring.dto.DockerContainerState.RUNNING;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("서버 모니터링 서비스")
class MonitoringServiceTest {

    @Mock
    private HostAgentClient hostAgentClient;

    @InjectMocks
    private MonitoringService monitoringService;

    @Test
    @DisplayName("Host Agent 상태를 그대로 반환한다")
    void returnsHostAgentStatus() {
        ServerStatusResponse expected = onlineResponse();

        when(hostAgentClient.getStatus()).thenReturn(expected);

        ServerStatusResponse response =
                monitoringService.getServerStatus();

        assertEquals(expected, response);
    }

    @Test
    @DisplayName("Host Agent 요청 실패 시 UNKNOWN을 반환한다")
    void returnsUnknownWhenAgentRequestFails() {
        when(hostAgentClient.getStatus()).thenThrow(
                new RestClientException("연결 실패")
        );

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

    @Test
    @DisplayName("Host Agent 컨테이너 목록을 그대로 반환한다")
    void returnsHostAgentContainers() {
        List<DockerContainerResponse> expected = List.of(
                new DockerContainerResponse(
                        "abc123",
                        "homehub-backend",
                        "homehub-backend:latest",
                        RUNNING,
                        "Up 2 hours (healthy)",
                        "127.0.0.1:8080->8080/tcp",
                        "homehub"
                )
        );

        when(hostAgentClient.getContainers())
                .thenReturn(expected);

        List<DockerContainerResponse> response =
                monitoringService.getDockerContainers();

        assertEquals(expected, response);
    }

    private ServerStatusResponse onlineResponse() {
        return new ServerStatusResponse(
                ONLINE,
                12.35,
                8,
                new ResourceUsageResponse(
                        1_000L,
                        600L,
                        400L,
                        60.0
                ),
                new ResourceUsageResponse(
                        2_000L,
                        1_500L,
                        500L,
                        75.0
                ),
                3_600L,
                OffsetDateTime.parse(
                        "2026-11-17T17:00:00+09:00"
                )
        );
    }
}
