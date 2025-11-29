package com.openforum.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.openforum")
@EnableJpaRepositories(basePackages = "com.openforum.infra.jpa.repository")
@EntityScan(basePackages = "com.openforum.infra.jpa.entity")
public class OpenForumApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenForumApplication.class, args);
    }

}
