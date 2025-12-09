package com.openforum.infra.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity extends TenantAwareEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String content;

    @Column(name = "reply_to_post_id")
    private UUID replyToPostId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mentioned_user_ids", columnDefinition = "jsonb")
    private List<UUID> mentionedUserIds;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "post_number")
    private Integer postNumber;

    @jakarta.persistence.Version
    @Column(nullable = false)
    private Long version;

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getReplyToPostId() {
        return replyToPostId; // Retaining getReplyToPostId
    }

    public void setReplyToPostId(UUID replyToPostId) {
        this.replyToPostId = replyToPostId;
    }

    public List<UUID> getMentionedUserIds() {
        return mentionedUserIds;
    }

    public void setMentionedUserIds(List<UUID> mentionedUserIds) {
        this.mentionedUserIds = mentionedUserIds;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getPostNumber() {
        return postNumber;
    }

    public void setPostNumber(Integer postNumber) {
        this.postNumber = postNumber;
    }
}
