package com.openforum.domain.aggregate;

import com.openforum.domain.events.ThreadClosed;
import com.openforum.domain.events.ThreadCreatedEvent;
import com.openforum.domain.events.ThreadTitleChanged;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ThreadHistoryTest {

    @Test
    void shouldEmitThreadCreatedEvent_whenThreadIsCreated() {
        // Given
        UUID id = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String tenantId = "tenant-1";
        String title = "Test Thread";

        // When
        Thread thread = Thread.builder()
                .id(id)
                .tenantId(tenantId)
                .authorId(authorId)
                .title(title)
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        List<Object> events = thread.pollEvents();

        // Then
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ThreadCreatedEvent.class);

        ThreadCreatedEvent event = (ThreadCreatedEvent) events.get(0);
        assertThat(event.threadId()).isEqualTo(id);
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.title()).isEqualTo(title);
        assertThat(event.authorId()).isEqualTo(authorId);
    }

    @Test
    void shouldEmitThreadTitleChangedEvent_whenTitleIsChanged() {
        // Given
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title("Original Title")
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        thread.pollEvents(); // Clear creation event

        UUID editorId = UUID.randomUUID();
        String newTitle = "Updated Title";

        // When
        thread.changeTitle(newTitle, editorId);

        List<Object> events = thread.pollEvents();

        // Then
        assertThat(thread.getTitle()).isEqualTo(newTitle);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ThreadTitleChanged.class);

        ThreadTitleChanged event = (ThreadTitleChanged) events.get(0);
        assertThat(event.oldTitle()).isEqualTo("Original Title");
        assertThat(event.newTitle()).isEqualTo(newTitle);
        assertThat(event.byMemberId()).isEqualTo(editorId);
        assertThat(event.threadId()).isEqualTo(thread.getId());
        assertThat(event.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void shouldNotEmitEvent_whenTitleIsUnchanged() {
        // Given
        String originalTitle = "Same Title";
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title(originalTitle)
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        thread.pollEvents(); // Clear creation event

        // When
        thread.changeTitle(originalTitle, UUID.randomUUID()); // Same title

        List<Object> events = thread.pollEvents();

        // Then
        assertThat(events).isEmpty();
    }

    @Test
    void shouldThrowException_whenChangingToNullTitle() {
        // Given
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title("Original Title")
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        // When/Then
        assertThatThrownBy(() -> thread.changeTitle(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowException_whenChangingToEmptyTitle() {
        // Given
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title("Original Title")
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        // When/Then
        assertThatThrownBy(() -> thread.changeTitle("   ", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldEmitThreadClosedEvent_whenThreadIsClosed() {
        // Given
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title("Thread to Close")
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        thread.pollEvents(); // Clear creation event

        UUID closerId = UUID.randomUUID();
        String reason = "Duplicate thread";

        // When
        thread.close(reason, closerId);

        List<Object> events = thread.pollEvents();

        // Then
        assertThat(thread.getStatus()).isEqualTo(ThreadStatus.CLOSED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ThreadClosed.class);

        ThreadClosed event = (ThreadClosed) events.get(0);
        assertThat(event.reason()).isEqualTo(reason);
        assertThat(event.byMemberId()).isEqualTo(closerId);
        assertThat(event.threadId()).isEqualTo(thread.getId());
        assertThat(event.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void shouldThrowException_whenClosingAlreadyClosedThread() {
        // Given
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title("Thread to Close")
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        thread.close("First close", UUID.randomUUID());

        // When/Then
        assertThatThrownBy(() -> thread.close("Second close", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void shouldAllowMultipleTitleChanges() {
        // Given
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .authorId(UUID.randomUUID())
                .title("Title 1")
                .status(ThreadStatus.OPEN)
                .metadata(Map.of())
                .isNew(true)
                .build();

        thread.pollEvents(); // Clear creation event

        UUID editorId = UUID.randomUUID();

        // When
        thread.changeTitle("Title 2", editorId);
        thread.changeTitle("Title 3", editorId);

        List<Object> events = thread.pollEvents();

        // Then
        assertThat(thread.getTitle()).isEqualTo("Title 3");
        assertThat(events).hasSize(2);

        ThreadTitleChanged event1 = (ThreadTitleChanged) events.get(0);
        assertThat(event1.oldTitle()).isEqualTo("Title 1");
        assertThat(event1.newTitle()).isEqualTo("Title 2");

        ThreadTitleChanged event2 = (ThreadTitleChanged) events.get(1);
        assertThat(event2.oldTitle()).isEqualTo("Title 2");
        assertThat(event2.newTitle()).isEqualTo("Title 3");
    }
}
