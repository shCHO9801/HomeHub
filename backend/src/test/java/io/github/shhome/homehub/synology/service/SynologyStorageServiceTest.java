package io.github.shhome.homehub.synology.service;

import io.github.shhome.homehub.synology.client.SynologySnmpClient;
import io.github.shhome.homehub.synology.client.dto.SnmpStorageItemData;
import io.github.shhome.homehub.synology.client.dto.SnmpVolumeData;
import io.github.shhome.homehub.synology.client.dto.SynologySnmpSnapshot;
import io.github.shhome.homehub.synology.config.SynologySnmpProperties;
import io.github.shhome.homehub.synology.dto.SynologyStorageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.UNAVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyStorageHealth.CRITICAL;
import static io.github.shhome.homehub.synology.dto.SynologyStorageHealth.HEALTHY;
import static io.github.shhome.homehub.synology.dto.SynologyStorageHealth.WARNING;
import static io.github.shhome.homehub.synology.dto.SynologyStorageItemType.STORAGE_POOL;
import static io.github.shhome.homehub.synology.dto.SynologyStorageItemType.VOLUME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Synology 저장공간 서비스")
class SynologyStorageServiceTest {

    @Mock
    private SynologySnmpClient synologySnmpClient;

    @Test
    @DisplayName("SNMP 응답을 볼륨 사용량과 저장소 상태로 변환한다")
    void convertsSnmpSnapshotToStorageResponse() {
        when(synologySnmpClient.getSnapshot())
                .thenReturn(snapshot());

        SynologyStorageService service = service(
                Optional.of(synologySnmpClient),
                Duration.ofMinutes(5)
        );

        SynologyStorageResponse response =
                service.getStorageStatus();

        assertEquals(AVAILABLE, response.status());
        assertFalse(response.stale());
        assertNotNull(response.collectedAt());

        var volume = response.volumes().getFirst();
        assertEquals("Volume 1", volume.name());
        assertEquals("/volume1", volume.path());
        assertEquals(4_096_000L, volume.totalBytes());
        assertEquals(1_024_000L, volume.usedBytes());
        assertEquals(3_072_000L, volume.availableBytes());
        assertEquals(25.0, volume.usagePercent());

        assertEquals(VOLUME, response.storageItems().get(0).type());
        assertEquals(HEALTHY, response.storageItems().get(0).health());
        assertEquals(
                STORAGE_POOL,
                response.storageItems().get(1).type()
        );
        assertEquals(WARNING, response.storageItems().get(1).health());
        assertEquals(CRITICAL, response.storageItems().get(2).health());
    }

    @Test
    @DisplayName("SNMP 클라이언트가 없으면 연결 불가 상태를 반환한다")
    void returnsUnavailableWhenSnmpClientIsMissing() {
        SynologyStorageService service = service(
                Optional.empty(),
                Duration.ofMinutes(5)
        );

        SynologyStorageResponse response =
                service.getStorageStatus();

        assertEquals(UNAVAILABLE, response.status());
        assertFalse(response.stale());
        assertTrue(response.volumes().isEmpty());
        assertTrue(response.storageItems().isEmpty());
        assertNull(response.collectedAt());
    }

    @Test
    @DisplayName("최근 수집이 실패하면 마지막 성공 데이터를 stale 상태로 반환한다")
    void returnsLastSuccessfulResponseAsStaleWhenRefreshFails() {
        when(synologySnmpClient.getSnapshot())
                .thenReturn(snapshot())
                .thenThrow(new IllegalStateException("timeout"));

        SynologyStorageService service = service(
                Optional.of(synologySnmpClient),
                Duration.ZERO
        );

        SynologyStorageResponse successful =
                service.getStorageStatus();
        SynologyStorageResponse failed =
                service.getStorageStatus();

        assertEquals(UNAVAILABLE, failed.status());
        assertTrue(failed.stale());
        assertEquals(successful.volumes(), failed.volumes());
        assertEquals(
                successful.storageItems(),
                failed.storageItems()
        );
        assertEquals(
                successful.collectedAt(),
                failed.collectedAt()
        );
    }

    @Test
    @DisplayName("캐시 유효 시간에는 SNMP를 다시 조회하지 않는다")
    void reusesResponseWhileCacheIsValid() {
        when(synologySnmpClient.getSnapshot())
                .thenReturn(snapshot());

        SynologyStorageService service = service(
                Optional.of(synologySnmpClient),
                Duration.ofMinutes(5)
        );

        SynologyStorageResponse first =
                service.getStorageStatus();
        SynologyStorageResponse second =
                service.getStorageStatus();

        assertSame(first, second);
        verify(synologySnmpClient).getSnapshot();
    }

    private SynologyStorageService service(
            Optional<SynologySnmpClient> client,
            Duration cacheTtl
    ) {
        return new SynologyStorageService(
                client,
                new SynologySnmpProperties(
                        true,
                        "192.168.0.3",
                        161,
                        "homehub-monitor",
                        "auth-password",
                        "privacy-password",
                        Duration.ofSeconds(3),
                        1,
                        cacheTtl
                )
        );
    }

    private SynologySnmpSnapshot snapshot() {
        return new SynologySnmpSnapshot(
                List.of(new SnmpVolumeData(
                        "/volume1",
                        4_096L,
                        1_000L,
                        250L
                )),
                List.of(
                        new SnmpStorageItemData("Volume 1", 1),
                        new SnmpStorageItemData("Storage Pool 1", 11),
                        new SnmpStorageItemData("Volume 2", 12)
                )
        );
    }
}
