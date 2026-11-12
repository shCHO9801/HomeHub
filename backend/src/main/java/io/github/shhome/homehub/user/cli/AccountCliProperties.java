package io.github.shhome.homehub.user.cli;

import io.github.shhome.homehub.user.entity.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "homehub.account.cli")
public record AccountCliProperties(
        AccountAction action,
        String username,
        UserRole role,
        Path passwordFile
) {
}
