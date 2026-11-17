package io.github.shhome.homehub.agent.config;

import io.github.shhome.homehub.agent.security.AgentTokenAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AgentTokenAuthenticationFilter authenticationFilter
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/agent/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll()
                )
                .addFilterBefore(
                        authenticationFilter,
                        AnonymousAuthenticationFilter.class
                );

        return http.build();
    }
}
