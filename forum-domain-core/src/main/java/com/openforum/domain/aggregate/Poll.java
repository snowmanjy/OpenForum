package com.openforum.domain.aggregate;

import com.openforum.domain.events.PollCreatedEvent;
import com.openforum.domain.events.PollVoteCastEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Poll {

    private final UUID id;
    private final String tenantId;
    private final UUID postId;
    private final String question;
    private final List<String> options;
    private final Instant expiresAt;
    private final boolean allowMultipleVotes;
    private final Instant createdAt;
    private final List<PollVote> votes;
    private final List<Object> domainEvents = new ArrayList<>();

    private Poll(UUID id, String tenantId, UUID postId, String question, List<String> options, Instant expiresAt,
            boolean allowMultipleVotes, Instant createdAt, List<PollVote> votes) {
        this.id = id;
        this.tenantId = tenantId;
        this.postId = postId;
        this.question = question;
        this.options = new ArrayList<>(options);
        this.expiresAt = expiresAt;
        this.allowMultipleVotes = allowMultipleVotes;
        this.createdAt = createdAt;
        this.votes = new ArrayList<>(votes);
    }

    public static Poll create(String tenantId, UUID postId, String question, List<String> options, Instant expiresAt,
            boolean allowMultipleVotes) {
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("Poll must have at least 2 options");
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Poll expiration must be in the future");
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Poll poll = new Poll(id, tenantId, postId, question, options, expiresAt, allowMultipleVotes, now,
                new ArrayList<>());
        poll.domainEvents
                .add(new PollCreatedEvent(id, tenantId, postId, question, options, expiresAt, allowMultipleVotes, now));
        return poll;
    }

    public static Poll reconstitute(UUID id, String tenantId, UUID postId, String question, List<String> options,
            Instant expiresAt,
            boolean allowMultipleVotes, Instant createdAt, List<PollVote> votes) {
        return new Poll(id, tenantId, postId, question, options, expiresAt, allowMultipleVotes, createdAt, votes);
    }

    public void castVote(UUID voterId, int optionIndex) {
        if (isExpired()) {
            throw new IllegalStateException("Poll has expired");
        }
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index");
        }
        if (!allowMultipleVotes && hasVoted(voterId)) {
            throw new IllegalStateException("User has already voted");
        }

        PollVote vote = PollVote.create(this.id, voterId, optionIndex);
        this.votes.add(vote);
        this.domainEvents.add(new PollVoteCastEvent(vote.getId(), this.id, voterId, optionIndex, vote.getCreatedAt()));
    }

    public boolean hasVoted(UUID voterId) {
        return votes.stream().anyMatch(v -> v.getVoterId().equals(voterId));
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public List<Object> pollEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getPostId() {
        return postId;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isAllowMultipleVotes() {
        return allowMultipleVotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PollVote> getVotes() {
        return Collections.unmodifiableList(votes);
    }
}
