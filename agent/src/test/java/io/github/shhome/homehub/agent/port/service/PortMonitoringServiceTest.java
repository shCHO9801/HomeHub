package io.github.shhome.homehub.agent.port.service;

import io.github.shhome.homehub.agent.port.config.PortProperties;
import io.github.shhome.homehub.agent.port.dto.PortStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static io.github.shhome.homehub.agent.port.dto.PortExposure.ALL_INTERFACES;
import static io.github.shhome.homehub.agent.port.dto.PortExposure.LOOPBACK;
import static io.github.shhome.homehub.agent.port.dto.PortExposure.SPECIFIC_INTERFACE;
import static io.github.shhome.homehub.agent.port.dto.PortProtocol.TCP;
import static io.github.shhome.homehub.agent.port.dto.PortProtocol.UDP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("포트 모니터링 서비스")
class PortMonitoringServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("ss 출력을 포트 번호 순으로 변환한다")
    void convertsSsOutputToSortedPorts() throws IOException {
        Path command = createCommand("""
                cat <<'OUTPUT'
                tcp LISTEN 0 4096 127.0.0.1:8100 0.0.0.0:* users:(("docker-proxy",pid=10,fd=8))
                udp UNCONN 0 0 192.168.0.10%enp2s0:68 0.0.0.0:* users:(("systemd-network",pid=11,fd=22))
                tcp LISTEN 0 4096 0.0.0.0:22 0.0.0.0:* users:(("sshd",pid=12,fd=3))
                udp UNCONN 0 0 *:443 *:* users:(("caddy",pid=13,fd=8))
                tcp LISTEN 0 50 [::ffff:127.0.0.1]:41665 *:* users:(("java",pid=14,fd=351))
                tcp LISTEN 0 4096 127.0.0.1:9999 0.0.0.0:*
                OUTPUT
                """);

        PortMonitoringService service = createService(
                command,
                Duration.ofSeconds(1)
        );

        List<PortStatusResponse> ports = service.getPorts();

        assertEquals(6, ports.size());

        assertEquals(22, ports.get(0).port());
        assertEquals(TCP, ports.get(0).protocol());
        assertEquals(ALL_INTERFACES, ports.get(0).exposure());
        assertEquals("sshd", ports.get(0).processName());

        assertEquals(68, ports.get(1).port());
        assertEquals(UDP, ports.get(1).protocol());
        assertEquals(SPECIFIC_INTERFACE, ports.get(1).exposure());
        assertEquals("192.168.0.10%enp2s0", ports.get(1).bindAddress());

        assertEquals(443, ports.get(2).port());
        assertEquals(ALL_INTERFACES, ports.get(2).exposure());
        assertEquals("caddy", ports.get(2).processName());

        assertEquals(8100, ports.get(3).port());
        assertEquals(LOOPBACK, ports.get(3).exposure());

        assertEquals(9999, ports.get(4).port());
        assertNull(ports.get(4).processName());

        assertEquals(41665, ports.get(5).port());
        assertEquals(LOOPBACK, ports.get(5).exposure());
    }

    @Test
    @DisplayName("출력이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenOutputIsEmpty() throws IOException {
        Path command = createCommand("exit 0");

        PortMonitoringService service = createService(
                command,
                Duration.ofSeconds(1)
        );

        assertTrue(service.getPorts().isEmpty());
    }

    @Test
    @DisplayName("응답 필드가 부족하면 조회에 실패한다")
    void rejectsMalformedOutput() throws IOException {
        Path command = createCommand(
                "printf 'tcp LISTEN 잘못된-응답\\n'"
        );

        PortMonitoringService service = createService(
                command,
                Duration.ofSeconds(1)
        );

        assertThrows(
                IllegalStateException.class,
                service::getPorts
        );
    }

    @Test
    @DisplayName("명령 실행 시간이 초과되면 조회에 실패한다")
    void rejectsTimedOutCommand() throws IOException {
        Path command = createCommand("sleep 1");

        PortMonitoringService service = createService(
                command,
                Duration.ofMillis(50)
        );

        assertThrows(
                IllegalStateException.class,
                service::getPorts
        );
    }

    private PortMonitoringService createService(
            Path command,
            Duration timeout
    ) {
        return new PortMonitoringService(
                new PortProperties(
                        command.toString(),
                        timeout
                )
        );
    }

    private Path createCommand(String body) throws IOException {
        Path command = tempDirectory.resolve(
                "port-status-" + System.nanoTime()
        );

        Files.writeString(
                command,
                "#!/bin/sh\n" + body + "\n"
        );

        boolean executable = command.toFile().setExecutable(true);

        if (!executable) {
            throw new IllegalStateException(
                    "테스트 명령에 실행 권한을 설정하지 못했습니다."
            );
        }

        return command;
    }
}
