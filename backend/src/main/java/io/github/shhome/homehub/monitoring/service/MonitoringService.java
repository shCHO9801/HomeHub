package io.github.shhome.homehub.monitoring.service;

import io.github.shhome.homehub.monitoring.client.HostAgentClient;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static io.github.shhome.homehub.monitoring.dto.ServerStatus.UNKNOWN;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final HostAgentClient hostAgentClient;

    public ServerStatusResponse getServerStatus() {
        try {
            return hostAgentClient.getStatus();
        } catch (RuntimeException exception) {
            log.warn(
                    "Host Agent 상태 조회에 실패했습니다. type={}",
                    exception.getClass().getSimpleName()
            );

            return new ServerStatusResponse(
                    UNKNOWN,
                    null,
                    null,
                    null,
                    null,
                    null,
                    OffsetDateTime.now()
            );
        }
    }

}
