package io.github.shhome.homehub.synology.client;

import io.github.shhome.homehub.synology.client.dto.SynologyApiInfoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class SynologyApiClient {

    private static final String REQUIRED_APIS =
            "SYNO.API.Auth,"
                    + "SYNO.FileStation.Info,"
                    + "SYNO.FileStation.List";

    private final RestClient restClient;

    public SynologyApiClient(
            @Qualifier("synologyRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public SynologyApiInfoResponse getApiInfo() {
        SynologyApiInfoResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/webapi/entry.cgi")
                        .queryParam(
                                "api",
                                "SYNO.API.Info"
                        )
                        .queryParam("version", 1)
                        .queryParam("method", "query")
                        .queryParam(
                                "query",
                                REQUIRED_APIS
                        )
                        .build()
                )
                .retrieve()
                .body(SynologyApiInfoResponse.class);

        return Objects.requireNonNull(
                response,
                "Synology API 응답 본문이 없습니다."
        );
    }
}