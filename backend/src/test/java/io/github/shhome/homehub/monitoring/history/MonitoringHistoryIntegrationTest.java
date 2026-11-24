package io.github.shhome.homehub.monitoring.history;

import io.github.shhome.homehub.monitoring.history.dto.MonitoringHistoryResponse;
import io.github.shhome.homehub.monitoring.history.service.MonitoringHistoryService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("서버 모니터링 이력 통합 테스트")
class MonitoringHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringHistoryService monitoringHistoryService;

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 최근 서버 모니터링 이력을 조회한다")
    void authenticatedUserGetsMonitoringHistory()
            throws Exception {
        when(monitoringHistoryService.getRecentHistory(24))
                .thenReturn(List.of(
                        new MonitoringHistoryResponse(
                                12.35,
                                45.68,
                                20.48,
                                OffsetDateTime.parse(
                                        "2026-11-24T00:00:00Z"
                                )
                        )
                ));

        mockMvc.perform(get("/api/monitoring/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpuUsagePercent")
                        .value(12.35))
                .andExpect(jsonPath("$[0].memoryUsagePercent")
                        .value(45.68))
                .andExpect(jsonPath("$[0].diskUsagePercent")
                        .value(20.48))
                .andExpect(jsonPath("$[0].collectedAt")
                        .value("2026-11-24T00:00:00Z"));
    }

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("조회 시간은 1시간 이상 720시간 이하여야 한다")
    void historyHoursMustBeWithinSupportedRange()
            throws Exception {
        mockMvc.perform(get("/api/monitoring/history")
                        .param("hours", "721"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("조회 시간은 720시간 이하여야 합니다."))
                .andExpect(jsonPath("$.field")
                        .value("hours"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 서버 모니터링 이력을 조회할 수 없다")
    void unauthenticatedUserCannotGetMonitoringHistory()
            throws Exception {
        mockMvc.perform(get("/api/monitoring/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_02"));
    }
}
