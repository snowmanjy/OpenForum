package com.openforum.domain.aggregate;

import com.openforum.domain.events.PostContentEdited;
import com.openforum.domain.events.PostCreatedEvent;
import com.openforum.domain.events.PostDeleted;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PostHistoryTest {

    @Test
    void shouldEmitPostCreatedEvent_whenPostIsCreated() {
        // Given
        UUID id = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String tenantId = "tenant-1";
        String content = "Test post content";

        // When
        Post post = Post.builder()
                .id(id)
                .threadId(threadId)
                .tenantId(tenantId)
                .authorId(authorId)
                .content(content)
                .isNew(true)
                .isBot(false)
                .build();

        List<Object> events = post.pollEvents();

        // Then
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PostCreatedEvent.class);

        PostCreatedEvent event = (PostCreatedEvent) events.get(0);
        assertThat(event.postId()).isEqualTo(id);
        assertThat(event.threadId()).isEqualTo(threadId);
        assertThat(event.authorId()).isEqualTo(authorId);
        assertThat(event.content()).isEqualTo(content);
    }

    @Test
    void shouldEmitPostContentEditedEvent_whenContentIsEdited() {
        // Given
        String originalContent = "Original content";
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content(originalContent)
                .isNew(true)
                .isBot(false)
                .build();

        post.pollEvents(); // Clear creation event

        UUID editorId = UUID.randomUUID();
        String newContent = "Updated content";

        // When
        post.editContent(newContent, editorId);

        List<Object> events = post.pollEvents();

        // Then
        assertThat(post.getContent()).isEqualTo(newContent);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PostContentEdited.class);

        PostContentEdited event = (PostContentEdited) events.get(0);
        assertThat(event.oldContent()).isEqualTo(originalContent);
        assertThat(event.newContent()).isEqualTo(newContent);
        assertThat(event.byMemberId()).isEqualTo(editorId);
        assertThat(event.postId()).isEqualTo(post.getId());
        assertThat(event.threadId()).isEqualTo(post.getThreadId());
        assertThat(event.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void shouldNotEmitEvent_whenContentIsUnchanged() {
        // Given
        String originalContent = "Same content";
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content(originalContent)
                .isNew(true)
                .isBot(false)
                .build();

        post.pollEvents(); // Clear creation event

        // When
        post.editContent(originalContent, UUID.randomUUID()); // Same content

        List<Object> events = post.pollEvents();

        // Then
        assertThat(events).isEmpty();
    }

    @Test
    void shouldThrowException_whenEditingToNullContent() {
        // Given
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content("Original content")
                .isNew(true)
                .isBot(false)
                .build();

        // When/Then
        assertThatThrownBy(() -> post.editContent(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowException_whenEditingToEmptyContent() {
        // Given
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content("Original content")
                .isNew(true)
                .isBot(false)
                .build();

        // When/Then
        assertThatThrownBy(() -> post.editContent("   ", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldEmitPostDeletedEvent_whenPostIsDeleted() {
        // Given
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content("Post to delete")
                .isNew(true)
                .isBot(false)
                .build();

        post.pollEvents(); // Clear creation event

        UUID deleterId = UUID.randomUUID();
        String reason = "Spam";

        // When
        post.delete(reason, deleterId);

        List<Object> events = post.pollEvents();

        // Then
        assertThat(post.isDeleted()).isTrue();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PostDeleted.class);

        PostDeleted event = (PostDeleted) events.get(0);
        assertThat(event.reason()).isEqualTo(reason);
        assertThat(event.byMemberId()).isEqualTo(deleterId);
        assertThat(event.postId()).isEqualTo(post.getId());
        assertThat(event.threadId()).isEqualTo(post.getThreadId());
        assertThat(event.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void shouldThrowException_whenDeletingAlreadyDeletedPost() {
        // Given
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content("Post to delete")
                .isNew(true)
                .isBot(false)
                .build();

        post.delete("First delete", UUID.randomUUID());

        // When/Then
        assertThatThrownBy(() -> post.delete("Second delete", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already deleted");
    }

    @Test
    void shouldThrowException_whenEditingDeletedPost() {
        // Given
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content("Post to delete")
                .isNew(true)
                .isBot(false)
                .build();

        post.delete("Deleted", UUID.randomUUID());

        // When/Then
        assertThatThrownBy(() -> post.editContent("New content", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot edit a deleted post");
    }

    @Test
    void shouldAllowMultipleEdits() {
        // Given
        Post post = Post.builder()
                .id(UUID.randomUUID())
                .threadId(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .content("Content 1")
                .isNew(true)
                .isBot(false)
                .build();

        post.pollEvents(); // Clear creation event

        UUID editorId = UUID.randomUUID();

        // When
        post.editContent("Content 2", editorId);
        post.editContent("Content 3", editorId);

        List<Object> events = post.pollEvents();

        // Then
        assertThat(post.getContent()).isEqualTo("Content 3");
        assertThat(events).hasSize(2);

        PostContentEdited event1 = (PostContentEdited) events.get(0);
        assertThat(event1.oldContent()).isEqualTo("Content 1");
        assertThat(event1.newContent()).isEqualTo("Content 2");

        PostContentEdited event2 = (PostContentEdited) events.get(1);
        assertThat(event2.oldContent()).isEqualTo("Content 2");
        assertThat(event2.newContent()).isEqualTo("Content 3");
    }
}
