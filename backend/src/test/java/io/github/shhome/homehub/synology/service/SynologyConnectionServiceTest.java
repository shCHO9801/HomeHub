package io.github.shhome.homehub.synology.service;

import io.github.shhome.homehub.synology.client.SynologyApiClient;
import io.github.shhome.homehub.synology.client.dto.SynologyApiInfo;
import io.github.shhome.homehub.synology.client.dto.SynologyApiInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Synology 연결 서비스")
class SynologyConnectionServiceTest {

    @Mock
    private SynologyApiClient synologyApiClient;

    @InjectMocks
    private SynologyConnectionService synologyConnectionService;

    @Test
    @DisplayName("필수 API를 모두 지원하면 연결 가능 상태를 반환한다")
    void returnsAvailableWhenRequiredApisExist() {
        when(synologyApiClient.getApiInfo())
                .thenReturn(successResponse());

        var response =
                synologyConnectionService.getConnectionStatus();

        assertEquals(AVAILABLE, response.status());
        assertNotNull(response.checkedAt());
    }

    @Test
    @DisplayName("필수 API가 누락되면 연결 불가 상태를 반환한다")
    void returnsUnavailableWhenRequiredApiIsMissing() {
        var apiInfo = new SynologyApiInfo(
                "entry.cgi",
                1,
                7,
                null
        );

        when(synologyApiClient.getApiInfo())
                .thenReturn(new SynologyApiInfoResponse(
                        true,
                        Map.of("SYNO.API.Auth", apiInfo),
                        null
                ));

        var response =
                synologyConnectionService.getConnectionStatus();

        assertEquals(UNAVAILABLE, response.status());
        assertNotNull(response.checkedAt());
    }

    @Test
    @DisplayName("Synology 요청이 실패하면 연결 불가 상태를 반환한다")
    void returnsUnavailableWhenRequestFails() {
        when(synologyApiClient.getApiInfo())
                .thenThrow(
                        new RestClientException(
                                "connection failed"
                        )
                );

        var response =
                synologyConnectionService.getConnectionStatus();

        assertEquals(UNAVAILABLE, response.status());
        assertNotNull(response.checkedAt());
    }

    private SynologyApiInfoResponse successResponse() {
        var apiInfo = new SynologyApiInfo(
                "entry.cgi",
                1,
                7,
                null
        );

        return new SynologyApiInfoResponse(
                true,
                Map.of(
                        "SYNO.API.Auth",
                        apiInfo,
                        "SYNO.FileStation.Info",
                        apiInfo,
                        "SYNO.FileStation.List",
                        apiInfo
                ),
                null
        );
    }
}