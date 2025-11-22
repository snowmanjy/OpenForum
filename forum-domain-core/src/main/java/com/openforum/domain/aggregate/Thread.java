package com.openforum.domain.aggregate;

import com.openforum.domain.events.ThreadCreatedEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Thread {
    private final UUID id;
    private final String tenantId;
    private final UUID authorId;
    private String title;
    private ThreadStatus status;
    private final Map<String, Object> metadata;
    private Long version;

    private final List<Object> domainEvents = new ArrayList<>();

    public Thread(UUID id, String tenantId, UUID authorId, String title, Map<String, Object> metadata) {
        this(id, tenantId, authorId, title, ThreadStatus.OPEN, metadata, null);
        this.domainEvents.add(new ThreadCreatedEvent(id, tenantId, authorId, title, LocalDateTime.now()));
    }

    public Thread(UUID id, String tenantId, UUID authorId, String title, ThreadStatus status,
            Map<String, Object> metadata, Long version) {
        this.id = id;
        this.tenantId = tenantId;
        this.authorId = authorId;
        this.title = title;
        this.status = status;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.version = version;
    }

    public List<Object> pollEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public ThreadStatus getStatus() {
        return status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Long getVersion() {
        return version;
    }
}
