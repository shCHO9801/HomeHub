package io.github.shhome.homehub.synology;

import io.github.shhome.homehub.synology.dto.SynologyStorageItemResponse;
import io.github.shhome.homehub.synology.dto.SynologyStorageResponse;
import io.github.shhome.homehub.synology.dto.SynologyVolumeResponse;
import io.github.shhome.homehub.synology.service.SynologyStorageService;
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

import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyStorageHealth.HEALTHY;
import static io.github.shhome.homehub.synology.dto.SynologyStorageItemType.VOLUME;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Synology 저장공간 통합 테스트")
class SynologyStorageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SynologyStorageService synologyStorageService;

    @Test
    @WithMockUser(
            username = "test-member",
            roles = "MEMBER"
    )
    @DisplayName("인증된 사용자는 Synology 저장공간 상태를 조회한다")
    void authenticatedUserGetsStorageStatus() throws Exception {
        when(synologyStorageService.getStorageStatus())
                .thenReturn(response());

        mockMvc.perform(get("/api/synology/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.volumes[0].path")
                        .value("/volume1"))
                .andExpect(jsonPath("$.volumes[0].usagePercent")
                        .value(25.0))
                .andExpect(jsonPath("$.storageItems[0].health")
                        .value("HEALTHY"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 Synology 저장공간 상태를 조회할 수 없다")
    void unauthenticatedUserCannotGetStorageStatus()
            throws Exception {
        mockMvc.perform(get("/api/synology/storage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_02"));
    }

    private SynologyStorageResponse response() {
        return new SynologyStorageResponse(
                AVAILABLE,
                false,
                List.of(new SynologyVolumeResponse(
                        "Volume 1",
                        "/volume1",
                        4_096_000L,
                        1_024_000L,
                        3_072_000L,
                        25.0
                )),
                List.of(new SynologyStorageItemResponse(
                        "Volume 1",
                        VOLUME,
                        HEALTHY
                )),
                OffsetDateTime.parse(
                        "2026-11-23T23:47:03+09:00"
                )
        );
    }
}
