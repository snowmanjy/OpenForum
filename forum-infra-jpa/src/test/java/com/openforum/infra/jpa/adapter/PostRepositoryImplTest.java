package com.openforum.infra.jpa.adapter;

import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.PostFactory;
import com.openforum.domain.repository.PostRepository;
import com.openforum.infra.jpa.config.JpaTestConfig;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ContextConfiguration(classes = com.openforum.TestApplication.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import({ PostRepositoryImpl.class, com.openforum.infra.jpa.mapper.PostMapper.class, JpaTestConfig.class })
class PostRepositoryImplTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    void shouldSavePostAndPublishEvent() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Post post = PostFactory.create(threadId, authorId, "Test Content", null, Map.of("key", "value"), false);
        UUID postId = post.getId();

        // When
        postRepository.save(post);

        // Then
        Optional<Post> savedPost = postRepository.findById(postId);
        assertThat(savedPost).isPresent();
        assertThat(savedPost.get().getContent()).isEqualTo("Test Content");
        assertThat(savedPost.get().getMetadata()).containsEntry("key", "value");

        List<OutboxEventEntity> events = outboxEventJpaRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo("PostCreatedEvent");
    }

    @Test
    void shouldFindPostsByThreadId() {
        // Given
        UUID threadId = UUID.randomUUID();
        Post post1 = PostFactory.create(threadId, UUID.randomUUID(), "Post 1", null, Map.of(), false);
        Post post2 = PostFactory.create(threadId, UUID.randomUUID(), "Post 2", null, Map.of(), false);
        Post post3 = PostFactory.create(UUID.randomUUID(), UUID.randomUUID(), "Other Thread", null, Map.of(), false);

        postRepository.save(post1);
        postRepository.save(post2);
        postRepository.save(post3);

        // When
        List<Post> threadPosts = postRepository.findByThreadId(threadId, 10);

        // Then
        assertThat(threadPosts).hasSize(2);
        assertThat(threadPosts).extracting(Post::getContent).containsExactlyInAnyOrder("Post 1", "Post 2");
    }
}
