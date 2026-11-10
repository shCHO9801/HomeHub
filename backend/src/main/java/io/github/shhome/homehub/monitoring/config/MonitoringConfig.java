package io.github.shhome.homehub.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

@Configuration
public class MonitoringConfig {

    @Bean
    public SystemInfoProvider systemInfoProvider() {
        return SystemInfoFactory.create();
    }
}
