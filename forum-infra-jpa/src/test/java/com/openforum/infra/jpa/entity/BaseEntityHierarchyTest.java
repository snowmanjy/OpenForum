package com.openforum.infra.jpa.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the BaseEntity and TenantAwareEntity abstract classes.
 */
class BaseEntityHierarchyTest {

    // Concrete test implementation of BaseEntity
    static class TestBaseEntity extends BaseEntity {
    }

    // Concrete test implementation of TenantAwareEntity
    static class TestTenantAwareEntity extends TenantAwareEntity {
    }

    @Test
    void baseEntity_shouldStoreAndRetrieveId() {
        TestBaseEntity entity = new TestBaseEntity();
        UUID id = UUID.randomUUID();

        entity.setId(id);

        assertThat(entity.getId()).isEqualTo(id);
    }

    @Test
    void tenantAwareEntity_shouldExtendBaseEntity() {
        TestTenantAwareEntity entity = new TestTenantAwareEntity();

        // Verify inheritance
        assertThat(entity).isInstanceOf(BaseEntity.class);
    }

    @Test
    void tenantAwareEntity_shouldStoreAndRetrieveTenantId() {
        TestTenantAwareEntity entity = new TestTenantAwareEntity();
        String tenantId = "test-tenant";

        entity.setTenantId(tenantId);

        assertThat(entity.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void tenantAwareEntity_shouldInheritIdFromBaseEntity() {
        TestTenantAwareEntity entity = new TestTenantAwareEntity();
        UUID id = UUID.randomUUID();
        String tenantId = "test-tenant";

        entity.setId(id);
        entity.setTenantId(tenantId);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void threadEntity_shouldExtendTenantAwareEntity() {
        ThreadEntity entity = new ThreadEntity();

        assertThat(entity).isInstanceOf(TenantAwareEntity.class);
        assertThat(entity).isInstanceOf(BaseEntity.class);
    }

    @Test
    void postEntity_shouldExtendTenantAwareEntity() {
        PostEntity entity = new PostEntity();

        assertThat(entity).isInstanceOf(TenantAwareEntity.class);
        assertThat(entity).isInstanceOf(BaseEntity.class);
    }

    @Test
    void memberEntity_shouldExtendTenantAwareEntity() {
        MemberEntity entity = new MemberEntity();

        assertThat(entity).isInstanceOf(TenantAwareEntity.class);
        assertThat(entity).isInstanceOf(BaseEntity.class);
    }

    @Test
    void outboxEventEntity_shouldExtendBaseEntity_butNotTenantAwareEntity() {
        OutboxEventEntity entity = new OutboxEventEntity();

        assertThat(entity).isInstanceOf(BaseEntity.class);
        assertThat(entity).isNotInstanceOf(TenantAwareEntity.class);
    }
}
