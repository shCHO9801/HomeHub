package io.github.shhome.homehub.agent.docker;

import io.github.shhome.homehub.agent.docker.dto.DockerContainerResponse;
import io.github.shhome.homehub.agent.docker.service.DockerMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.github.shhome.homehub.agent.docker.dto.DockerContainerState.RUNNING;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "AGENT_TOKEN=test-agent-token")
@AutoConfigureMockMvc
@DisplayName("Docker 모니터링 API 통합 테스트")
class DockerMonitoringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DockerMonitoringService dockerMonitoringService;

    @Test
    @DisplayName("올바른 토큰으로 컨테이너 목록을 조회한다")
    void validTokenReturnsContainers() throws Exception {
        when(dockerMonitoringService.getContainers())
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

        mockMvc.perform(get("/api/agent/docker/containers")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer test-agent-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("homehub-backend"))
                .andExpect(jsonPath("$[0].state")
                        .value("RUNNING"))
                .andExpect(jsonPath("$[0].ports")
                        .value("127.0.0.1:8080->8080/tcp"))
                .andExpect(content().string(
                        not(containsString("test-agent-token"))
                ));
    }

    @Test
    @DisplayName("토큰이 없으면 컨테이너 목록 조회를 거부한다")
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/agent/docker/containers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}