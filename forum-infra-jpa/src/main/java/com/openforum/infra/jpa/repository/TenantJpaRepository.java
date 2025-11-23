package com.openforum.infra.jpa.repository;

import com.openforum.infra.jpa.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, String> {
}
