package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.BookmarkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BookmarkJpaRepository extends JpaRepository<BookmarkEntity, UUID> {

    Optional<BookmarkEntity> findByMemberIdAndPostId(UUID memberId, UUID postId);

    boolean existsByMemberIdAndPostId(UUID memberId, UUID postId);

    /**
     * Get all post IDs bookmarked by a member (for batch checking isBookmarked).
     */
    @Query("SELECT b.postId FROM BookmarkEntity b WHERE b.memberId = :memberId")
    Set<UUID> findPostIdsByMemberId(@Param("memberId") UUID memberId);

    /**
     * Get paginated bookmarks for a member.
     */
    Page<BookmarkEntity> findByMemberId(UUID memberId, Pageable pageable);

    /**
     * Delete bookmark by member and post (for unbookmark operation).
     */
    void deleteByMemberIdAndPostId(UUID memberId, UUID postId);
}
