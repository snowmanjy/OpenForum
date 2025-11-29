package com.openforum.domain.aggregate;

import com.openforum.domain.events.PollCreatedEvent;
import com.openforum.domain.events.PollVoteCastEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PollTest {

    @Test
    void shouldCreatePoll() {
        String tenantId = "tenant-1";
        UUID postId = UUID.randomUUID();
        String question = "What is your favorite color?";
        List<String> options = List.of("Red", "Blue", "Green");
        Instant expiresAt = Instant.now().plusSeconds(3600);
        boolean allowMultipleVotes = false;

        Poll poll = Poll.create(tenantId, postId, question, options, expiresAt, allowMultipleVotes);

        assertThat(poll.getId()).isNotNull();
        assertThat(poll.getTenantId()).isEqualTo(tenantId);
        assertThat(poll.getPostId()).isEqualTo(postId);
        assertThat(poll.getQuestion()).isEqualTo(question);
        assertThat(poll.getOptions()).containsExactlyElementsOf(options);
        assertThat(poll.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(poll.isAllowMultipleVotes()).isFalse();
        assertThat(poll.getCreatedAt()).isNotNull();
        assertThat(poll.getVotes()).isEmpty();

        List<Object> events = poll.pollEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PollCreatedEvent.class);
    }

    @Test
    void shouldCastVote() {
        Poll poll = Poll.create("tenant-1", UUID.randomUUID(), "Question?", List.of("A", "B"),
                Instant.now().plusSeconds(3600), false);
        UUID voterId = UUID.randomUUID();

        poll.castVote(voterId, 0);

        assertThat(poll.getVotes()).hasSize(1);
        PollVote vote = poll.getVotes().get(0);
        assertThat(vote.getVoterId()).isEqualTo(voterId);
        assertThat(vote.getOptionIndex()).isEqualTo(0);

        List<Object> events = poll.pollEvents();
        assertThat(events).hasSize(2); // Created + VoteCast
        assertThat(events.get(1)).isInstanceOf(PollVoteCastEvent.class);
    }

    @Test
    void shouldPreventDoubleVotingWhenNotAllowed() {
        Poll poll = Poll.create("tenant-1", UUID.randomUUID(), "Question?", List.of("A", "B"),
                Instant.now().plusSeconds(3600), false);
        UUID voterId = UUID.randomUUID();

        poll.castVote(voterId, 0);

        assertThatThrownBy(() -> poll.castVote(voterId, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User has already voted");
    }

    @Test
    void shouldAllowMultipleVotesWhenAllowed() {
        Poll poll = Poll.create("tenant-1", UUID.randomUUID(), "Question?", List.of("A", "B"),
                Instant.now().plusSeconds(3600), true);
        UUID voterId = UUID.randomUUID();

        poll.castVote(voterId, 0);
        poll.castVote(voterId, 1);

        assertThat(poll.getVotes()).hasSize(2);
    }

    @Test
    void shouldPreventVotingOnExpiredPoll() {
        // Use reconstitute to bypass creation validation and simulate an existing
        // expired poll
        Poll poll = Poll.reconstitute(
                UUID.randomUUID(),
                "tenant-1",
                UUID.randomUUID(),
                "Question?",
                List.of("A", "B"),
                Instant.now().minusSeconds(1),
                false,
                Instant.now().minusSeconds(3600),
                List.of());
        UUID voterId = UUID.randomUUID();

        assertThatThrownBy(() -> poll.castVote(voterId, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Poll has expired");
    }

    @Test
    void shouldValidateOptions() {
        assertThatThrownBy(() -> Poll.create("t", UUID.randomUUID(), "Q", List.of("A"), null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Poll must have at least 2 options");
    }
}
