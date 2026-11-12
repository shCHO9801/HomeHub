package io.github.shhome.homehub.synology;

import io.github.shhome.homehub.synology.dto.SynologyConnectionResponse;
import io.github.shhome.homehub.synology.service.SynologyConnectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Synology 연결 통합 테스트")
class SynologyConnectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SynologyConnectionService synologyConnectionService;

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 Synology 연결 상태를 조회한다")
    void authenticatedUserGetsConnectionStatus()
            throws Exception {
        when(synologyConnectionService.getConnectionStatus())
                .thenReturn(new SynologyConnectionResponse(
                        AVAILABLE,
                        OffsetDateTime.parse(
                                "2026-11-12T22:00:00+09:00"
                        )
                ));

        mockMvc.perform(get("/api/synology/connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.checkedAt")
                        .isNotEmpty());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 Synology 연결 상태를 조회할 수 없다")
    void unauthenticatedUserCannotGetConnectionStatus()
            throws Exception {
        mockMvc.perform(get("/api/synology/connection"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_02"));
    }
}