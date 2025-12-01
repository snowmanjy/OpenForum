package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 200 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    java.util.List<OutboxEventEntity> findTop200ByOrderByCreatedAtAsc();
}
