package io.github.shhome.homehub.agent.port.service;

import io.github.shhome.homehub.agent.port.config.PortProperties;
import io.github.shhome.homehub.agent.port.dto.PortExposure;
import io.github.shhome.homehub.agent.port.dto.PortProtocol;
import io.github.shhome.homehub.agent.port.dto.PortStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PortMonitoringService {

    private static final int MINIMUM_FIELD_COUNT = 6;

    private static final Pattern PROCESS_NAME_PATTERN =
            Pattern.compile("\"([^\"]+)\"");

    private final PortProperties properties;

    public List<PortStatusResponse> getPorts() {
        String output = executeStatusCommand();

        if (output.isBlank()) {
            return List.of();
        }

        return output.lines()
                .filter(line -> !line.isBlank())
                .map(this::toResponse)
                .sorted(
                        Comparator
                                .comparingInt(PortStatusResponse::port)
                                .thenComparing(PortStatusResponse::protocol)
                                .thenComparing(
                                        PortStatusResponse::bindAddress
                                )
                )
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
                        "포트 상태 조회 시간이 초과되었습니다."
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
                        "포트 상태 조회에 실패했습니다. "
                                + errorOutput.strip()
                );
            }

            return standardOutput;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "포트 상태 조회 명령을 실행할 수 없습니다.",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "포트 상태 조회가 중단되었습니다.",
                    exception
            );
        }
    }

    private PortStatusResponse toResponse(String line) {
        String[] fields = line
                .strip()
                .split("\\s+", 7);

        if (fields.length < MINIMUM_FIELD_COUNT) {
            throw new IllegalStateException(
                    "포트 상태 응답 형식이 올바르지 않습니다."
            );
        }

        PortProtocol protocol = PortProtocol.valueOf(
                fields[0].toUpperCase(Locale.ROOT)
        );

        String localEndpoint = fields[4];
        int separatorIndex = localEndpoint.lastIndexOf(':');

        if (separatorIndex <= 0
                || separatorIndex == localEndpoint.length() - 1) {
            throw new IllegalStateException(
                    "포트 주소 형식이 올바르지 않습니다."
            );
        }

        String bindAddress = localEndpoint.substring(
                0,
                separatorIndex
        );

        int port;

        try {
            port = Integer.parseInt(
                    localEndpoint.substring(separatorIndex + 1)
            );
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "포트 번호 형식이 올바르지 않습니다.",
                    exception
            );
        }

        String processDetails =
                fields.length == 7 ? fields[6] : "";

        return new PortStatusResponse(
                protocol,
                port,
                bindAddress,
                toExposure(bindAddress),
                extractProcessName(processDetails)
        );
    }

    private PortExposure toExposure(String bindAddress) {
        String normalized = bindAddress
                .toLowerCase(Locale.ROOT);

        if (normalized.startsWith("[")
                && normalized.endsWith("]")) {
            normalized = normalized.substring(
                    1,
                    normalized.length() - 1
            );
        }

        int interfaceSeparator = normalized.indexOf('%');

        if (interfaceSeparator >= 0) {
            normalized = normalized.substring(
                    0,
                    interfaceSeparator
            );
        }

        if ("0.0.0.0".equals(normalized)
                || "::".equals(normalized)
                || "*".equals(normalized)) {
            return PortExposure.ALL_INTERFACES;
        }

        if (normalized.startsWith("127.")
                || "::1".equals(normalized)
                || normalized.startsWith("::ffff:127.")) {
            return PortExposure.LOOPBACK;
        }

        return PortExposure.SPECIFIC_INTERFACE;
    }

    private String extractProcessName(String processDetails) {
        Matcher matcher =
                PROCESS_NAME_PATTERN.matcher(processDetails);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }
}