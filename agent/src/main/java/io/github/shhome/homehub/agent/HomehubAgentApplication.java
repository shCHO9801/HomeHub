package io.github.shhome.homehub.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HomehubAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                HomehubAgentApplication.class,
                args
        );
    }
}
