package com.openforum.domain.aggregate;

import com.openforum.domain.events.PrivatePostCreatedEvent;
import com.openforum.domain.events.PrivateThreadCreatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrivateThreadTest {

    @Test
    void shouldCreatePrivateThread() {
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        List<UUID> participants = List.of(participant1, participant2);
        String tenantId = "tenant-1";
        String subject = "Secret Plans";

        PrivateThread thread = PrivateThread.create(tenantId, participants, subject);

        assertThat(thread.getId()).isNotNull();
        assertThat(thread.getTenantId()).isEqualTo(tenantId);
        assertThat(thread.getParticipantIds()).containsExactlyInAnyOrder(participant1, participant2);
        assertThat(thread.getSubject()).isEqualTo(subject);
        assertThat(thread.getCreatedAt()).isNotNull();
        assertThat(thread.getLastActivityAt()).isEqualTo(thread.getCreatedAt());
        assertThat(thread.getPosts()).isEmpty();

        List<Object> events = thread.pollEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PrivateThreadCreatedEvent.class);
    }

    @Test
    void shouldFailToCreateWithLessThanTwoParticipants() {
        UUID participant1 = UUID.randomUUID();
        List<UUID> participants = List.of(participant1);

        assertThatThrownBy(() -> PrivateThread.create("tenant-1", participants, "Subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Private thread must have at least 2 participants");
    }

    @Test
    void shouldAddPost() {
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        List<UUID> participants = List.of(participant1, participant2);
        PrivateThread thread = PrivateThread.create("tenant-1", participants, "Subject");
        thread.pollEvents(); // Clear creation event

        String content = "Hello there!";
        thread.addPost(content, participant1);

        assertThat(thread.getPosts()).hasSize(1);
        PrivatePost post = thread.getPosts().get(0);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getAuthorId()).isEqualTo(participant1);
        assertThat(thread.getLastActivityAt()).isEqualTo(post.getCreatedAt());

        List<Object> events = thread.pollEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PrivatePostCreatedEvent.class);
    }

    @Test
    void shouldFailToAddPostByNonParticipant() {
        UUID participant1 = UUID.randomUUID();
        UUID participant2 = UUID.randomUUID();
        List<UUID> participants = List.of(participant1, participant2);
        PrivateThread thread = PrivateThread.create("tenant-1", participants, "Subject");

        UUID intruder = UUID.randomUUID();
        assertThatThrownBy(() -> thread.addPost("I am hacking", intruder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Author is not a participant of this private thread");
    }
}
