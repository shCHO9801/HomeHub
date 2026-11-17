package io.github.shhome.homehub.monitoring.client;

import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class HostAgentClient {

    private final RestClient restClient;

    public HostAgentClient(
            @Qualifier("hostAgentRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public ServerStatusResponse getStatus() {
        ServerStatusResponse response = restClient
                .get()
                .uri("/api/agent/status")
                .retrieve()
                .body(ServerStatusResponse.class);

        return Objects.requireNonNull(
                response,
                "Host Agent 응답 본문이 없습니다."
        );
    }
}
