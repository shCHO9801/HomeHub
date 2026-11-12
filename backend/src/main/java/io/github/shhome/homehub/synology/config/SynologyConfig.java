package io.github.shhome.homehub.synology.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(SynologyProperties.class)
public class SynologyConfig {

    @Bean
    @Qualifier("synologyRestClient")
    public RestClient synologyRestClient(
            RestClient.Builder builder,
            SynologyProperties properties
    ) {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        return builder.baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
