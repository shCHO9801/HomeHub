package io.github.shhome.homehub.agent.port;

import io.github.shhome.homehub.agent.port.dto.PortStatusResponse;
import io.github.shhome.homehub.agent.port.service.PortMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.github.shhome.homehub.agent.port.dto.PortExposure.LOOPBACK;
import static io.github.shhome.homehub.agent.port.dto.PortProtocol.TCP;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "AGENT_TOKEN=test-agent-token")
@AutoConfigureMockMvc
@DisplayName("포트 모니터링 API 통합 테스트")
class PortMonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortMonitoringService portMonitoringService;

    @Test
    @DisplayName("올바른 토큰으로 사용 중인 포트를 조회한다")
    void validTokenReturnsPorts() throws Exception {
        when(portMonitoringService.getPorts())
                .thenReturn(List.of(
                        new PortStatusResponse(
                                TCP,
                                8100,
                                "127.0.0.1",
                                LOOPBACK,
                                "docker-proxy"
                        )
                ));

        mockMvc.perform(get("/api/agent/ports")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer test-agent-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocol")
                        .value("TCP"))
                .andExpect(jsonPath("$[0].port")
                        .value(8100))
                .andExpect(jsonPath("$[0].exposure")
                        .value("LOOPBACK"))
                .andExpect(jsonPath("$[0].processName")
                        .value("docker-proxy"))
                .andExpect(content().string(
                        not(containsString("test-agent-token"))
                ));
    }

    @Test
    @DisplayName("토큰이 없으면 포트 조회를 거부한다")
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/agent/ports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
