package com.openforum.rest.service;

import com.openforum.domain.aggregate.Bookmark;
import com.openforum.domain.aggregate.Post;
import com.openforum.infra.jpa.entity.BookmarkEntity;
import com.openforum.infra.jpa.mapper.PostMapper;
import com.openforum.infra.jpa.repository.BookmarkJpaRepository;
import com.openforum.infra.jpa.repository.PostJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Service for managing post bookmarks.
 * Bookmarks are distinct from upvotes and don't trigger notifications.
 */
@Service
@Transactional
public class BookmarkService {

    private final BookmarkJpaRepository bookmarkRepository;
    private final PostJpaRepository postRepository;
    private final PostMapper postMapper;

    public BookmarkService(
            BookmarkJpaRepository bookmarkRepository,
            PostJpaRepository postRepository,
            PostMapper postMapper) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    /**
     * Bookmarks a post for the given member.
     * Idempotent - does nothing if already bookmarked.
     */
    public void bookmarkPost(UUID memberId, UUID postId, String tenantId) {
        // Check if already bookmarked (idempotent)
        if (bookmarkRepository.existsByMemberIdAndPostId(memberId, postId)) {
            return;
        }

        // Create and save bookmark
        Bookmark bookmark = Bookmark.create(tenantId, memberId, postId);
        BookmarkEntity entity = toEntity(bookmark);
        bookmarkRepository.save(entity);

        // Increment post bookmark count
        postRepository.findById(postId).ifPresent(post -> {
            Integer currentCount = post.getBookmarkCount();
            post.setBookmarkCount((currentCount != null ? currentCount : 0) + 1);
            postRepository.save(post);
        });
    }

    /**
     * Removes a bookmark for the given member.
     * Idempotent - does nothing if not bookmarked.
     */
    public void unbookmarkPost(UUID memberId, UUID postId) {
        bookmarkRepository.findByMemberIdAndPostId(memberId, postId)
                .ifPresent(bookmark -> {
                    bookmarkRepository.delete(bookmark);

                    // Decrement post bookmark count
                    postRepository.findById(postId).ifPresent(post -> {
                        Integer currentCount = post.getBookmarkCount();
                        if (currentCount != null && currentCount > 0) {
                            post.setBookmarkCount(currentCount - 1);
                            postRepository.save(post);
                        }
                    });
                });
    }

    /**
     * Gets all post IDs bookmarked by a member.
     * Used for efficiently checking isBookmarked in batch.
     */
    @Transactional(readOnly = true)
    public Set<UUID> getBookmarkedPostIds(UUID memberId) {
        return bookmarkRepository.findPostIdsByMemberId(memberId);
    }

    /**
     * Gets paginated bookmarks for a member.
     */
    @Transactional(readOnly = true)
    public Page<Post> getMemberBookmarks(UUID memberId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return bookmarkRepository.findByMemberId(memberId, pageRequest)
                .map(bookmark -> postRepository.findById(bookmark.getPostId())
                        .map(postMapper::toDomain)
                        .orElse(null))
                .map(post -> post); // Filter nulls handled by caller
    }

    /**
     * Checks if a member has bookmarked a specific post.
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(UUID memberId, UUID postId) {
        return bookmarkRepository.existsByMemberIdAndPostId(memberId, postId);
    }

    private BookmarkEntity toEntity(Bookmark bookmark) {
        BookmarkEntity entity = new BookmarkEntity();
        entity.setId(bookmark.getId());
        entity.setTenantId(bookmark.getTenantId());
        entity.setMemberId(bookmark.getMemberId());
        entity.setPostId(bookmark.getPostId());
        entity.setCreatedAt(bookmark.getCreatedAt());
        return entity;
    }
}
