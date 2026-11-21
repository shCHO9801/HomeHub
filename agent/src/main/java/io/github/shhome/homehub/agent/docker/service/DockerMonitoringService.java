package io.github.shhome.homehub.agent.docker.service;

import io.github.shhome.homehub.agent.docker.config.DockerProperties;
import io.github.shhome.homehub.agent.docker.dto.DockerContainerResponse;
import io.github.shhome.homehub.agent.docker.dto.DockerContainerState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DockerMonitoringService {

    private static final int FIELD_COUNT = 7;

    private final DockerProperties properties;

    public List<DockerContainerResponse> getContainers() {
        String output = executeStatusCommand();

        if (output.isBlank()) {
            return List.of();
        }

        return output.lines()
                .filter(line -> !line.isBlank())
                .map(this::toResponse)
                .toList();
    }

    private String executeStatusCommand() {
        Process process;

        try {
            process = new ProcessBuilder(
                    properties.statusCommand()
            ).start();

            boolean completed = process.waitFor(
                    properties.commandTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!completed) {
                process.destroyForcibly();

                throw new IllegalStateException(
                        "Docker 상태 조회 시간이 초과되었습니다."
                );
            }

            String standardOutput = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String errorOutput = new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "Docker 상태 조회에 실패했습니다. "
                                + errorOutput.strip()
                );
            }

            return standardOutput;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Docker 상태 조회 명령을 실행할 수 없습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Docker 상태 조회가 중단되었습니다.",
                    exception
            );
        }
    }

    private DockerContainerResponse toResponse(String line) {
        String[] fields = line.split("\t", -1);

        if (fields.length != FIELD_COUNT) {
            throw new IllegalStateException(
                    "Docker 상태 응답 형식이 올바르지 않습니다."
            );
        }

        return new DockerContainerResponse(
                fields[0],
                fields[1],
                fields[2],
                toState(fields[3], fields[4]),
                fields[4],
                fields[5],
                fields[6]
        );
    }

    private DockerContainerState toState(
            String state,
            String statusDetail
    ) {
        String normalizedState =
                state.toLowerCase(Locale.ROOT);

        String normalizedStatus =
                statusDetail.toLowerCase(Locale.ROOT);

        if (normalizedStatus.contains("(unhealthy)")) {
            return DockerContainerState.UNHEALTHY;
        }

        return switch (normalizedState) {
            case "running" -> DockerContainerState.RUNNING;

            case "created", "exited", "dead", "paused" -> DockerContainerState.STOPPED;

            default -> DockerContainerState.UNKNOWN;
        };
    }
}