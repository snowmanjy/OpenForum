package com.openforum.domain.repository;

import com.openforum.domain.aggregate.Tenant;
import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);

    Optional<Tenant> findById(String id);

    Optional<Tenant> findBySlug(String slug);
}
