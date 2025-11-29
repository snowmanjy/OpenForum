package com.openforum.boot.config;

import com.openforum.domain.factory.PollFactory;
import com.openforum.domain.factory.PrivateThreadFactory;
import com.openforum.domain.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public PrivateThreadFactory privateThreadFactory(MemberRepository memberRepository) {
        return new PrivateThreadFactory(memberRepository);
    }

    @Bean
    public PollFactory pollFactory() {
        return new PollFactory();
    }
}
