package com.openforum;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.openforum.infra.jpa.entity")
@EnableJpaRepositories(basePackages = "com.openforum.infra.jpa.repository")
public class TestApplication {
}
