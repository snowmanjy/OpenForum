package com.openforum.admin.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.openforum.admin",
        "com.openforum.domain",
        "com.openforum.infra.jpa"
})
@EntityScan(basePackages = "com.openforum.infra.jpa.entity")
@EnableJpaRepositories(basePackages = "com.openforum.infra.jpa.repository")
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
