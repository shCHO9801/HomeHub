package io.github.shhome.homehub.agent.monitoring;

import io.github.shhome.homehub.agent.monitoring.dto.AgentStatusResponse;
import io.github.shhome.homehub.agent.monitoring.dto.ResourceUsageResponse;
import io.github.shhome.homehub.agent.monitoring.service.HostMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static io.github.shhome.homehub.agent.monitoring.dto.AgentStatus.ONLINE;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "AGENT_TOKEN=test-agent-token")
@AutoConfigureMockMvc
@DisplayName("Agent 상태 API 통합 테스트")
class AgentStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HostMonitoringService hostMonitoringService;

    @Test
    @DisplayName("올바른 토큰으로 호스트 상태를 조회한다")
    void validTokenReturnsHostStatus() throws Exception {
        when(hostMonitoringService.getStatus())
                .thenReturn(onlineResponse());

        mockMvc.perform(get("/api/agent/status")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer test-agent-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.cpuUsagePercent")
                        .value(12.35))
                .andExpect(content().string(
                        not(containsString("test-agent-token"))
                ));
    }

    @Test
    @DisplayName("토큰이 없으면 요청을 거부한다")
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/agent/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Agent 인증에 실패했습니다."));
    }

    @Test
    @DisplayName("토큰이 다르면 요청을 거부한다")
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/agent/status")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer wrong-token"
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private AgentStatusResponse onlineResponse() {
        return new AgentStatusResponse(
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
