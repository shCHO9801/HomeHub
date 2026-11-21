package io.github.shhome.homehub.agent.docker.service;

import io.github.shhome.homehub.agent.docker.config.DockerProperties;
import io.github.shhome.homehub.agent.docker.dto.DockerContainerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static io.github.shhome.homehub.agent.docker.dto.DockerContainerState.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Docker 모니터링 서비스")
class DockerMonitoringServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("스크립트 출력을 컨테이너 상태 목록으로 변환한다")
    void convertsCommandOutputToContainers() throws IOException {
        Path command = createCommand("""
                printf 'abc123\\tbackend\\thomehub-backend\\trunning\\tUp 2 hours (healthy)\\t127.0.0.1:8080->8080/tcp\\thomehub\\n'
                printf 'def456\\tdatabase\\tmariadb:11.8\\trunning\\tUp 2 hours (unhealthy)\\t3306/tcp\\thomehub\\n'
                printf 'ghi789\\tworker\\tworker:latest\\texited\\tExited (0) 1 hour ago\\t\\t\\n'
                """);

        DockerMonitoringService service = createService(
                command,
                Duration.ofSeconds(1)
        );

        List<DockerContainerResponse> containers =
                service.getContainers();

        assertEquals(3, containers.size());

        DockerContainerResponse running = containers.get(0);
        assertEquals("backend", running.name());
        assertEquals("homehub-backend", running.image());
        assertEquals(RUNNING, running.state());
        assertEquals(
                "127.0.0.1:8080->8080/tcp",
                running.ports()
        );
        assertEquals("homehub", running.composeProject());

        assertEquals(UNHEALTHY, containers.get(1).state());
        assertEquals(STOPPED, containers.get(2).state());
    }

    @Test
    @DisplayName("출력이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenOutputIsEmpty() throws IOException {
        Path command = createCommand("exit 0");

        DockerMonitoringService service = createService(
                command,
                Duration.ofSeconds(1)
        );

        assertTrue(service.getContainers().isEmpty());
    }

    @Test
    @DisplayName("응답 필드 개수가 다르면 조회에 실패한다")
    void rejectsMalformedOutput() throws IOException {
        Path command = createCommand(
                "printf '잘못된\\t응답\\n'"
        );

        DockerMonitoringService service = createService(
                command,
                Duration.ofSeconds(1)
        );

        assertThrows(
                IllegalStateException.class,
                service::getContainers
        );
    }

    @Test
    @DisplayName("명령 실행 시간이 초과되면 조회에 실패한다")
    void rejectsTimedOutCommand() throws IOException {
        Path command = createCommand("sleep 1");

        DockerMonitoringService service = createService(
                command,
                Duration.ofMillis(50)
        );

        assertThrows(
                IllegalStateException.class,
                service::getContainers
        );
    }

    private DockerMonitoringService createService(
            Path command,
            Duration timeout
    ) {
        return new DockerMonitoringService(
                new DockerProperties(
                        command.toString(),
                        timeout
                )
        );
    }

    private Path createCommand(String body) throws IOException {
        Path command = tempDirectory.resolve(
                "docker-status-" + System.nanoTime()
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