package com.openforum.domain.aggregate;

import com.openforum.domain.events.PrivatePostCreatedEvent;
import com.openforum.domain.events.PrivateThreadCreatedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PrivateThread {

    private final UUID id;
    private final String tenantId;
    private final List<UUID> participantIds;
    private final String subject;
    private final Instant createdAt;
    private Instant lastActivityAt;
    private final List<PrivatePost> posts;
    private final List<Object> domainEvents = new ArrayList<>();

    private PrivateThread(UUID id, String tenantId, List<UUID> participantIds, String subject, Instant createdAt,
            Instant lastActivityAt, List<PrivatePost> posts) {
        this.id = id;
        this.tenantId = tenantId;
        this.participantIds = participantIds;
        this.subject = subject;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.posts = new ArrayList<>(posts);
    }

    // Factory method for creating a new PrivateThread
    public static PrivateThread create(String tenantId, List<UUID> participantIds, String subject) {
        if (participantIds == null || participantIds.size() < 2) {
            throw new IllegalArgumentException("Private thread must have at least 2 participants");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject cannot be null or empty");
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        PrivateThread thread = new PrivateThread(id, tenantId, new ArrayList<>(participantIds), subject, now, now,
                new ArrayList<>());
        thread.domainEvents.add(new PrivateThreadCreatedEvent(id, tenantId, participantIds, subject, now));
        return thread;
    }

    // Reconstitute from persistence
    public static PrivateThread reconstitute(UUID id, String tenantId, List<UUID> participantIds, String subject,
            Instant createdAt, Instant lastActivityAt, List<PrivatePost> posts) {
        return new PrivateThread(id, tenantId, participantIds, subject, createdAt, lastActivityAt, posts);
    }

    public void addPost(String content, UUID authorId) {
        if (!participantIds.contains(authorId)) {
            throw new IllegalArgumentException("Author is not a participant of this private thread");
        }

        PrivatePost post = PrivatePost.create(this.id, authorId, content);
        this.posts.add(post);
        this.lastActivityAt = post.getCreatedAt();

        this.domainEvents
                .add(new PrivatePostCreatedEvent(post.getId(), this.id, authorId, content, post.getCreatedAt()));
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

    public List<UUID> getParticipantIds() {
        return Collections.unmodifiableList(participantIds);
    }

    public String getSubject() {
        return subject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public List<PrivatePost> getPosts() {
        return Collections.unmodifiableList(posts);
    }
}
