package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.infra.jpa.config.JpaTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Import({ MemberRepositoryImpl.class, com.openforum.infra.jpa.mapper.MemberMapper.class, JpaTestConfig.class })
class MemberRepositoryImplTest {

    @Container
    @ServiceConnection
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void should_save_and_retrieve_member() {
        // Given
        String externalId = "ext-123";
        Member member = Member.create(externalId, "test@example.com", "Test User", false);

        // When
        memberRepository.save(member);
        Optional<Member> retrieved = memberRepository.findByExternalId("tenant-1", externalId);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getExternalId()).isEqualTo(externalId);
        assertThat(retrieved.get().getEmail()).isEqualTo("test@example.com");
        assertThat(retrieved.get().getName()).isEqualTo("Test User");
    }

    @Test
    void should_findById() {
        // Given
        Member member = Member.create("ext-456", "john@example.com", "John Doe", false);
        memberRepository.save(member);
        UUID memberId = member.getId();

        // When
        Optional<Member> retrieved = memberRepository.findById(memberId);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(memberId);
    }

    @Test
    void should_search_by_handle_or_name() {
        // Given
        String tenantId = "tenant-1";
        memberRepository.save(Member.create("ext-1", "alice@example.com", "Alice Smith", false));
        memberRepository.save(Member.create("ext-2", "bob@example.com", "Bob Johnson", false));
        memberRepository.save(Member.create("ext-3", "charlie@example.com", "Charlie Brown", false));

        // When
        List<Member> results = memberRepository.searchByHandleOrName(tenantId, "ali", 10);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Alice Smith");
    }
}
