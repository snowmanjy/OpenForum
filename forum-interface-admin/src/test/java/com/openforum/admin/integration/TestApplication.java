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
        "com.openforum.infra.jpa",
        "com.openforum.application"
})
@EntityScan(basePackages = "com.openforum.infra.jpa.entity")
@EnableJpaRepositories(basePackages = "com.openforum.infra.jpa.repository")
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.PollFactory pollFactory() {
        return new com.openforum.domain.factory.PollFactory();
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.CategoryFactory categoryFactory() {
        return new com.openforum.domain.factory.CategoryFactory();
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.PostFactory postFactory() {
        return new com.openforum.domain.factory.PostFactory();
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.PrivateThreadFactory privateThreadFactory(
            com.openforum.domain.repository.MemberRepository memberRepository) {
        return new com.openforum.domain.factory.PrivateThreadFactory(memberRepository);
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.TagFactory tagFactory() {
        return new com.openforum.domain.factory.TagFactory();
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.TenantFactory tenantFactory() {
        return new com.openforum.domain.factory.TenantFactory();
    }

    @org.springframework.context.annotation.Bean
    public com.openforum.domain.factory.ThreadFactory threadFactory() {
        return new com.openforum.domain.factory.ThreadFactory();
    }
}
