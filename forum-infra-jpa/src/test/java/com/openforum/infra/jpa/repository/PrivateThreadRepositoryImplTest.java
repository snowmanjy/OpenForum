package com.openforum.infra.jpa.repository;

import com.openforum.domain.aggregate.PrivateThread;
import com.openforum.domain.repository.PrivateThreadRepository;
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
@Import({ PrivateThreadRepositoryImpl.class, com.openforum.infra.jpa.mapper.PrivateThreadMapper.class,
        JpaTestConfig.class })
class PrivateThreadRepositoryImplTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PrivateThreadRepository privateThreadRepository;

    @Test
    void shouldSaveAndFindPrivateThread() {
        // Given
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        List<UUID> participants = List.of(participant1, participant2);
        String tenantId = "tenant-repo-test";
        String subject = "Repo Test";

        PrivateThread thread = PrivateThread.create(tenantId, participants, subject);
        thread.addPost("First Message", participant1);

        // When
        privateThreadRepository.save(thread);

        // Then
        List<PrivateThread> foundThreads = privateThreadRepository.findByParticipantId(tenantId, participant1, 0, 10);
        assertThat(foundThreads).hasSize(1);
        PrivateThread foundThread = foundThreads.get(0);
        assertThat(foundThread.getId()).isEqualTo(thread.getId());
        assertThat(foundThread.getSubject()).isEqualTo(subject);
        assertThat(foundThread.getParticipantIds()).containsExactlyInAnyOrder(participant1, participant2);
        assertThat(foundThread.getPosts()).hasSize(1);
        assertThat(foundThread.getPosts().get(0).getContent()).isEqualTo("First Message");

        // Verify isolation
        List<PrivateThread> intruderThreads = privateThreadRepository.findByParticipantId(tenantId, UUID.randomUUID(),
                0, 10);
        assertThat(intruderThreads).isEmpty();
    }
}
