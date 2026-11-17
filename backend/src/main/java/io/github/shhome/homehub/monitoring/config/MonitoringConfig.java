package io.github.shhome.homehub.monitoring.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(HostAgentProperties.class)
public class MonitoringConfig {

    @Bean
    @Qualifier("hostAgentRestClient")
    public RestClient hostAgentRestClient(
            RestClient.Builder builder,
            HostAgentProperties properties
    ) {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient.Builder configuredBuilder = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory);

        if (properties.token() != null
                && !properties.token().isBlank()) {
            configuredBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + properties.token()
            );
        }

        return configuredBuilder.build();
    }
}
