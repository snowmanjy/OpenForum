package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadFactory;
import com.openforum.domain.repository.ThreadRepository;
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
import java.util.Map;
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
@Import({ ThreadRepositoryImpl.class, com.openforum.infra.jpa.mapper.ThreadMapper.class, JpaTestConfig.class })
class ThreadRepositorySearchTest {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

        @Autowired
        private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        @Autowired
        private ThreadRepository threadRepository;

        @Test
        void shouldSearchThreadsByTitle() {
                // Given
                UUID authorId = UUID.randomUUID();
                jdbcTemplate.update("INSERT INTO members (id, external_id, email, name, is_bot) VALUES (?, ?, ?, ?, ?)",
                                authorId, "ext-" + authorId, "test@example.com", "Test User", false);

                String tenantId = "tenant-search";
                Thread thread1 = ThreadFactory.create(tenantId, authorId, null, "Java Spring Boot Guide", Map.of());
                Thread thread2 = ThreadFactory.create(tenantId, authorId, null, "Python Data Science", Map.of());
                Thread thread3 = ThreadFactory.create(tenantId, authorId, null, "Advanced Java Concurrency", Map.of());
                Thread thread4 = ThreadFactory.create("other-tenant", authorId, null, "Java Basics", Map.of());

                threadRepository.saveAll(List.of(thread1, thread2, thread3, thread4));

                // When
                List<Thread> javaResults = threadRepository.search(tenantId, "Java", 0, 10);
                List<Thread> springResults = threadRepository.search(tenantId, "Spring", 0, 10);
                List<Thread> pythonResults = threadRepository.search(tenantId, "Python", 0, 10);
                List<Thread> emptyResults = threadRepository.search(tenantId, "Ruby", 0, 10);

                // Then
                assertThat(javaResults).hasSize(2)
                                .extracting(Thread::getTitle)
                                .containsExactlyInAnyOrder("Java Spring Boot Guide", "Advanced Java Concurrency");

                assertThat(springResults).hasSize(1)
                                .extracting(Thread::getTitle)
                                .containsExactly("Java Spring Boot Guide");

                assertThat(pythonResults).hasSize(1)
                                .extracting(Thread::getTitle)
                                .containsExactly("Python Data Science");

                assertThat(emptyResults).isEmpty();
        }
}
