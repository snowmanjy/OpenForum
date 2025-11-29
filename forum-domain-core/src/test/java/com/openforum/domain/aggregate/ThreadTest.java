package com.openforum.domain.aggregate;

import com.openforum.domain.events.ThreadCreatedEvent;
import com.openforum.domain.factory.ThreadFactory;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ThreadTest {

    @Test
    void should_accumulate_event_on_creation() {
        UUID authorId = UUID.randomUUID();
        String tenantId = "tenant-1";
        String title = "Test Thread";
        Map<String, Object> metadata = Map.of("key", "value");

        Thread thread = ThreadFactory.create(tenantId, authorId, null, title, metadata);

        List<Object> events = thread.pollEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ThreadCreatedEvent.class);

        ThreadCreatedEvent event = (ThreadCreatedEvent) events.get(0);
        assertThat(event.threadId()).isEqualTo(thread.getId());
        assertThat(event.title()).isEqualTo(title);
    }

    @Test
    void should_clear_events_after_polling() {
        Thread thread = ThreadFactory.create("t1", UUID.randomUUID(), null, "Title", Map.of());

        assertThat(thread.pollEvents()).isNotEmpty();
        assertThat(thread.pollEvents()).isEmpty();
    }

    @Test
    void shouldAddPostSuccessfully() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Thread thread = Thread.builder()
                .id(threadId)
                .tenantId("tenant123")
                .authorId(UUID.randomUUID())
                .title("Test Thread")
                .status(ThreadStatus.OPEN)
                .build();

        // When
        Post createdPost = thread.addPost("Test post content", authorId, false);

        // Then
        assertThat(createdPost).isNotNull();
        assertThat(createdPost.getThreadId()).isEqualTo(threadId);
        assertThat(createdPost.getAuthorId()).isEqualTo(authorId);
        assertThat(createdPost.getContent()).isEqualTo("Test post content");
    }

    @Test
    void shouldCreatePostWithCorrectThreadId() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Thread thread = Thread.builder()
                .id(threadId)
                .tenantId("tenant123")
                .authorId(UUID.randomUUID())
                .title("Test Thread")
                .status(ThreadStatus.OPEN)
                .build();

        // When
        Post createdPost = thread.addPost("Test post content", authorId, true);

        // Then - Thread creates the Post with correct threadId
        assertThat(createdPost.getThreadId()).isEqualTo(threadId);
    }

    @Test
    void shouldThrowExceptionWhenAddingPostToClosedThread() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Thread thread = Thread.builder()
                .id(threadId)
                .tenantId("tenant123")
                .authorId(UUID.randomUUID())
                .title("Test Thread")
                .status(ThreadStatus.CLOSED) // Thread is closed
                .build();

        // When/Then
        assertThatThrownBy(() -> thread.addPost("Test post content", authorId, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot add post to a closed thread.");
    }

    @Test
    void should_return_new_list_on_poll() {
        Thread thread = ThreadFactory.create("t1", UUID.randomUUID(), null, "Title", Map.of());

        List<Object> events1 = thread.pollEvents();
        List<Object> events2 = thread.pollEvents();

        assertThat(events1).isNotSameAs(events2);
    }
}
