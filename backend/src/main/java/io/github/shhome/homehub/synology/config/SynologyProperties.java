package io.github.shhome.homehub.synology.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "homehub.synology")
public record SynologyProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
