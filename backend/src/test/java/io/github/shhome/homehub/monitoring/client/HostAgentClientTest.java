package io.github.shhome.homehub.monitoring.client;

import com.sun.net.httpserver.HttpServer;
import io.github.shhome.homehub.monitoring.dto.DockerContainerResponse;
import io.github.shhome.homehub.monitoring.dto.PortStatusResponse;
import io.github.shhome.homehub.monitoring.dto.ServerStatusResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.shhome.homehub.monitoring.dto.DockerContainerState.RUNNING;
import static io.github.shhome.homehub.monitoring.dto.PortExposure.LOOPBACK;
import static io.github.shhome.homehub.monitoring.dto.PortProtocol.TCP;
import static io.github.shhome.homehub.monitoring.dto.ServerStatus.ONLINE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Host Agent 클라이언트")
class HostAgentClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Bearer 토큰을 보내고 Agent 상태를 읽는다")
    void sendsBearerTokenAndReadsStatus() throws IOException {
        AtomicReference<String> authorization =
                new AtomicReference<>();

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext("/api/agent/status", exchange -> {
            authorization.set(
                    exchange.getRequestHeaders()
                            .getFirst(HttpHeaders.AUTHORIZATION)
            );

            byte[] body = responseBody().getBytes(
                    StandardCharsets.UTF_8
            );

            exchange.getResponseHeaders().set(
                    HttpHeaders.CONTENT_TYPE,
                    "application/json"
            );
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.start();

        RestClient restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:"
                        + server.getAddress().getPort())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer test-agent-token"
                )
                .build();

        HostAgentClient client =
                new HostAgentClient(restClient);

        ServerStatusResponse response = client.getStatus();

        assertEquals(
                "Bearer test-agent-token",
                authorization.get()
        );
        assertEquals(ONLINE, response.status());
        assertEquals(12.35, response.cpuUsagePercent());
        assertEquals(600L, response.memory().usedBytes());
        assertEquals(1_500L, response.disk().usedBytes());
        assertEquals(3_600L, response.uptimeSeconds());
    }

    @Test
    @DisplayName("Agent 컨테이너 목록을 읽는다")
    void readsDockerContainers() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/api/agent/docker/containers",
                exchange -> {
                    byte[] body = """
                        [
                          {
                            "id": "abc123",
                            "name": "homehub-backend",
                            "image": "homehub-backend:latest",
                            "state": "RUNNING",
                            "statusDetail": "Up 2 hours (healthy)",
                            "ports": "127.0.0.1:8080->8080/tcp",
                            "composeProject": "homehub"
                          }
                        ]
                        """.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set(
                            HttpHeaders.CONTENT_TYPE,
                            "application/json"
                    );

                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                }
        );

        server.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(
                        "http://127.0.0.1:"
                                + server.getAddress().getPort()
                )
                .build();

        HostAgentClient client =
                new HostAgentClient(restClient);

        List<DockerContainerResponse> response =
                client.getContainers();

        assertEquals(1, response.size());
        assertEquals("homehub-backend", response.getFirst().name());
        assertEquals(RUNNING, response.getFirst().state());
        assertEquals(
                "127.0.0.1:8080->8080/tcp",
                response.getFirst().ports()
        );
    }

    @Test
    @DisplayName("Agent 포트 목록을 읽는다")
    void readsPorts() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext("/api/agent/ports", exchange -> {
            byte[] body = """
                    [
                      {
                        "protocol": "TCP",
                        "port": 8100,
                        "bindAddress": "127.0.0.1",
                        "exposure": "LOOPBACK",
                        "processName": "docker-proxy"
                      }
                    ]
                    """.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    HttpHeaders.CONTENT_TYPE,
                    "application/json"
            );
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(
                        "http://127.0.0.1:"
                                + server.getAddress().getPort()
                )
                .build();

        HostAgentClient client =
                new HostAgentClient(restClient);

        List<PortStatusResponse> response =
                client.getPorts();

        assertEquals(1, response.size());
        assertEquals(TCP, response.getFirst().protocol());
        assertEquals(8100, response.getFirst().port());
        assertEquals(LOOPBACK, response.getFirst().exposure());
        assertEquals(
                "docker-proxy",
                response.getFirst().processName()
        );
    }

    private String responseBody() {
        return """
                {
                  "status": "ONLINE",
                  "cpuUsagePercent": 12.35,
                  "logicalProcessorCount": 8,
                  "memory": {
                    "totalBytes": 1000,
                    "usedBytes": 600,
                    "availableBytes": 400,
                    "usagePercent": 60.0
                  },
                  "disk": {
                    "totalBytes": 2000,
                    "usedBytes": 1500,
                    "availableBytes": 500,
                    "usagePercent": 75.0
                  },
                  "uptimeSeconds": 3600,
                  "collectedAt": "2026-11-17T17:00:00+09:00"
                }
                """;
    }
}
