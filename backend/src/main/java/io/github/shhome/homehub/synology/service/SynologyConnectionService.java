package io.github.shhome.homehub.synology.service;

import io.github.shhome.homehub.synology.client.SynologyApiClient;
import io.github.shhome.homehub.synology.client.dto.SynologyApiInfoResponse;
import io.github.shhome.homehub.synology.dto.SynologyConnectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Set;

import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.AVAILABLE;
import static io.github.shhome.homehub.synology.dto.SynologyConnectionStatus.UNAVAILABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynologyConnectionService {

    private static final Set<String> REQUIRED_APIS = Set.of(
            "SYNO.API.Auth",
            "SYNO.FileStation.Info",
            "SYNO.FileStation.List"
    );

    private final SynologyApiClient synologyApiClient;

    public SynologyConnectionResponse getConnectionStatus() {
        try {
            SynologyApiInfoResponse response =
                    synologyApiClient.getApiInfo();

            boolean available =
                    response.success()
                            && response.data() != null
                            && response.data()
                            .keySet()
                            .containsAll(REQUIRED_APIS);

            return new SynologyConnectionResponse(
                    available ? AVAILABLE : UNAVAILABLE,
                    OffsetDateTime.now()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Synology connection check failed. cause={}",
                    exception.getClass().getSimpleName()
            );

            return new SynologyConnectionResponse(
                    UNAVAILABLE,
                    OffsetDateTime.now()
            );
        }
    }
}