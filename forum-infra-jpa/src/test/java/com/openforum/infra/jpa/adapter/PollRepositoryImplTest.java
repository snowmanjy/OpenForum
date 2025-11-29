package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Poll;
import com.openforum.domain.factory.PollFactory;
import com.openforum.domain.repository.PollRepository;
import com.openforum.infra.jpa.config.JpaTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration"
})
@Import({ PollRepositoryImpl.class, com.openforum.infra.jpa.mapper.PollMapper.class, JpaTestConfig.class })
@ActiveProfiles("test")
class PollRepositoryImplTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PollRepository pollRepository;

    private final PollFactory pollFactory = new PollFactory();

    @Test
    void shouldSaveAndFindPoll() {
        String tenantId = "tenant-1";
        UUID postId = UUID.randomUUID();
        Poll poll = pollFactory.create(tenantId, postId, "Question?", List.of("A", "B"),
                Instant.now().plusSeconds(3600), false);

        pollRepository.save(poll);

        Optional<Poll> found = pollRepository.findById(poll.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(poll.getId());
        assertThat(found.get().getTenantId()).isEqualTo(tenantId);
        assertThat(found.get().getPostId()).isEqualTo(postId);
        assertThat(found.get().getQuestion()).isEqualTo("Question?");
        assertThat(found.get().getOptions()).containsExactly("A", "B");
        assertThat(found.get().isAllowMultipleVotes()).isFalse();
    }

    @Test
    void shouldSaveAndFindPollWithVotes() {
        String tenantId = "tenant-1";
        UUID postId = UUID.randomUUID();
        Poll poll = pollFactory.create(tenantId, postId, "Question?", List.of("A", "B"),
                Instant.now().plusSeconds(3600), true);
        UUID voterId = UUID.randomUUID();
        poll.castVote(voterId, 0);
        poll.castVote(voterId, 1);

        pollRepository.save(poll);

        Optional<Poll> found = pollRepository.findById(poll.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getVotes()).hasSize(2);
        assertThat(found.get().getVotes()).extracting("optionIndex").containsExactlyInAnyOrder(0, 1);
    }
}
