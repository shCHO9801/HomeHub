package io.github.shhome.homehub.synology.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "homehub.synology.snmp")
public record SynologySnmpProperties (
        boolean enabled,
        String host,
        int port,
        String username,
        String authPassword,
        String privPassword,
        Duration timeout,
        int retries,
        Duration cacheTtl

){
}
