package io.github.shhome.homehub.monitoring.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "homehub.host-agent")
public record HostAgentProperties(
        @NotNull URI baseUrl,
        String token,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
}
