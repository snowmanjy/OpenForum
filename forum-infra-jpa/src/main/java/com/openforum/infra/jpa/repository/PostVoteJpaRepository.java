package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.PostVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostVoteJpaRepository extends JpaRepository<PostVoteEntity, UUID> {

    Optional<PostVoteEntity> findByPostIdAndMemberId(UUID postId, UUID memberId);

    List<PostVoteEntity> findByPostIdIn(List<UUID> postIds);

    @Query("SELECT pv FROM PostVoteEntity pv WHERE pv.postId IN :postIds AND pv.memberId = :memberId")
    List<PostVoteEntity> findByPostIdInAndMemberId(@Param("postIds") List<UUID> postIds, @Param("memberId") UUID memberId);

    @Modifying
    @Query("UPDATE PostEntity p SET p.score = p.score + :delta WHERE p.id = :postId")
    int updatePostScore(@Param("postId") UUID postId, @Param("delta") int delta);
}
