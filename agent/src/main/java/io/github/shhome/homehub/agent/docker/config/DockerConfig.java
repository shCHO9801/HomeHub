package io.github.shhome.homehub.agent.docker.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DockerProperties.class)
public class DockerConfig {
}