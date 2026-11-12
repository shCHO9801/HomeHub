package io.github.shhome.homehub.user.cli;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("account-cli")
@EnableConfigurationProperties(AccountCliProperties.class)
public class AccountCliConfig {
}
