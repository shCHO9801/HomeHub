package io.github.shhome.homehub.monitoring;

import io.github.shhome.homehub.monitoring.dto.DockerContainerResponse;
import io.github.shhome.homehub.monitoring.dto.ResourceUsageResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import io.github.shhome.homehub.monitoring.service.MonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static io.github.shhome.homehub.monitoring.dto.DockerContainerState.RUNNING;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("서버 모니터링 통합 테스트")
class MonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringService monitoringService;

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 현재 서버 상태를 조회한다")
    void authenticatedUserGetsServerStatus() throws Exception {
        when(monitoringService.getServerStatus())
                .thenReturn(onlineResponse());

        mockMvc.perform(get("/api/monitoring/server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.cpuUsagePercent").isNumber())
                .andExpect(jsonPath("$.logicalProcessorCount")
                        .isNumber())
                .andExpect(jsonPath("$.memory.totalBytes")
                        .isNumber())
                .andExpect(jsonPath("$.memory.usedBytes")
                        .isNumber())
                .andExpect(jsonPath("$.memory.availableBytes")
                        .isNumber())
                .andExpect(jsonPath("$.memory.usagePercent")
                        .isNumber())
                .andExpect(jsonPath("$.disk.totalBytes")
                        .isNumber())
                .andExpect(jsonPath("$.disk.usedBytes")
                        .isNumber())
                .andExpect(jsonPath("$.disk.availableBytes")
                        .isNumber())
                .andExpect(jsonPath("$.disk.usagePercent")
                        .isNumber())
                .andExpect(jsonPath("$.uptimeSeconds")
                        .isNumber())
                .andExpect(jsonPath("$.collectedAt")
                        .isNotEmpty());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 서버 상태를 조회할 수 없다")
    void unauthenticatedUserCannotGetServerStatus()
            throws Exception {
        mockMvc.perform(get("/api/monitoring/server"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_02"));
    }

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 Docker 컨테이너 목록을 조회한다")
    void authenticatedUserGetsDockerContainers() throws Exception {
        when(monitoringService.getDockerContainers())
                .thenReturn(List.of(
                        new DockerContainerResponse(
                                "abc123",
                                "homehub-backend",
                                "homehub-backend:latest",
                                RUNNING,
                                "Up 2 hours (healthy)",
                                "127.0.0.1:8080->8080/tcp",
                                "homehub"
                        )
                ));

        mockMvc.perform(
                        get("/api/monitoring/docker/containers")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("homehub-backend"))
                .andExpect(jsonPath("$[0].state")
                        .value("RUNNING"))
                .andExpect(jsonPath("$[0].ports")
                        .value("127.0.0.1:8080->8080/tcp"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 Docker 컨테이너 목록을 조회할 수 없다")
    void unauthenticatedUserCannotGetDockerContainers()
            throws Exception {
        mockMvc.perform(
                        get("/api/monitoring/docker/containers")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_02"));
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
