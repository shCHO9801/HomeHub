package io.github.shhome.homehub.agent.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "homehub.agent")
public record AgentProperties(
        @NotBlank String token
) {
}
