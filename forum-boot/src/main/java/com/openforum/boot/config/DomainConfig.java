package com.openforum.boot.config;

import com.openforum.domain.factory.PollFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public PollFactory pollFactory() {
        return new PollFactory();
    }
}
