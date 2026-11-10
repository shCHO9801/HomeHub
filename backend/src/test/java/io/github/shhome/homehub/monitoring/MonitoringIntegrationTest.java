package io.github.shhome.homehub.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("서버 모니터링 통합 테스트")
class MonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 현재 서버 상태를 조회한다")
    void authenticatedUserGetsServerStatus() throws Exception {
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
}