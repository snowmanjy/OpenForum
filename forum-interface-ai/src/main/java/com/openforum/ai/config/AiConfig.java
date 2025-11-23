package com.openforum.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
@EnableAsync
public class AiConfig {

    @Bean
    public TextEncryptor textEncryptor() {
        // TODO: Replace with actual secure password and salt from environment variables
        // For production: Use Spring Cloud Config or AWS Secrets Manager
        String password = System.getenv().getOrDefault("ENCRYPTION_PASSWORD", "changeme");
        String salt = System.getenv().getOrDefault("ENCRYPTION_SALT", "deadbeef");
        return Encryptors.text(password, salt);
    }
}
