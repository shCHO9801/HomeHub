package io.github.shhome.homehub.monitoring.alert;

import io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertResponse;
import io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertSummaryResponse;
import io.github.shhome.homehub.monitoring.alert.service.MonitoringAlertService;
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

import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertSource.SERVER;
import static io.github.shhome.homehub.monitoring.alert.dto.MonitoringAlertStatus.WARNING;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("모니터링 경고 통합 테스트")
class MonitoringAlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringAlertService monitoringAlertService;

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 현재 모니터링 경고를 조회한다")
    void authenticatedUserGetsMonitoringAlerts()
            throws Exception {
        when(monitoringAlertService.getAlerts())
                .thenReturn(new MonitoringAlertSummaryResponse(
                        WARNING,
                        List.of(new MonitoringAlertResponse(
                                SERVER,
                                WARNING,
                                "디스크 사용률 경고",
                                "디스크 사용률이 90.0%입니다."
                        )),
                        OffsetDateTime.parse(
                                "2026-11-24T08:00:00Z"
                        )
                ));

        mockMvc.perform(get("/api/monitoring/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("WARNING"))
                .andExpect(jsonPath("$.alerts[0].source")
                        .value("SERVER"))
                .andExpect(jsonPath("$.alerts[0].title")
                        .value("디스크 사용률 경고"))
                .andExpect(jsonPath("$.checkedAt")
                        .value("2026-11-24T08:00:00Z"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 모니터링 경고를 조회할 수 없다")
    void unauthenticatedUserCannotGetMonitoringAlerts()
            throws Exception {
        mockMvc.perform(get("/api/monitoring/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_02"));
    }
}
