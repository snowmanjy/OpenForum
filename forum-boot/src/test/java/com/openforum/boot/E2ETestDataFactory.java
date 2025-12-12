package com.openforum.boot;

import com.openforum.infra.jpa.entity.TenantEntity;
import com.openforum.infra.jpa.entity.MemberEntity;
import com.openforum.infra.jpa.entity.CategoryEntity;
import com.openforum.infra.jpa.entity.ThreadEntity;
import com.openforum.infra.jpa.entity.PostEntity;
import com.openforum.infra.jpa.repository.TenantJpaRepository;
import com.openforum.infra.jpa.repository.MemberJpaRepository;
import com.openforum.infra.jpa.repository.CategoryJpaRepository;
import com.openforum.infra.jpa.repository.ThreadJpaRepository;
import com.openforum.infra.jpa.repository.PostJpaRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Shared helper class for E2E test data setup.
 * 
 * Provides factory methods to create the full entity chain:
 * Tenant → Member → Category → Thread → Post
 * 
 * This reduces code duplication across ThreadE2ETest, PostE2ETest, and
 * VoteE2ETest.
 */
public class E2ETestDataFactory {

    private final TenantJpaRepository tenantRepository;
    private final MemberJpaRepository memberRepository;
    private final CategoryJpaRepository categoryRepository;
    private final ThreadJpaRepository threadRepository;
    private final PostJpaRepository postRepository;

    public E2ETestDataFactory(
            TenantJpaRepository tenantRepository,
            MemberJpaRepository memberRepository,
            CategoryJpaRepository categoryRepository,
            ThreadJpaRepository threadRepository,
            PostJpaRepository postRepository) {
        this.tenantRepository = tenantRepository;
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
    }

    /**
     * Creates a tenant with the given ID.
     * 
     * @param tenantId The tenant ID (used as both ID and slug base)
     * @return The saved TenantEntity
     */
    public TenantEntity createTenant(String tenantId) {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);
        tenant.setSlug(tenantId + "-slug");
        tenant.setName("Test Tenant " + tenantId);
        tenant.setConfig(Map.of("theme", "dark"));
        return tenantRepository.save(tenant);
    }

    /**
     * Creates a member in the given tenant.
     * 
     * @param tenantId   The tenant ID
     * @param externalId The external ID (e.g., Clerk ID)
     * @return The saved MemberEntity with generated UUID
     */
    public MemberEntity createMember(String tenantId, String externalId) {
        UUID memberId = UUID.randomUUID();
        MemberEntity member = new MemberEntity(
                memberId,
                externalId,
                externalId + "@test.com",
                "Test User " + externalId,
                false,
                tenantId,
                Instant.now(),
                "MEMBER",
                null,
                0);
        return memberRepository.save(member);
    }

    /**
     * Creates a category in the given tenant.
     * 
     * @param tenantId The tenant ID
     * @param name     The category name
     * @return The saved CategoryEntity with generated UUID
     */
    public CategoryEntity createCategory(String tenantId, String name) {
        UUID categoryId = UUID.randomUUID();
        CategoryEntity category = new CategoryEntity();
        category.setId(categoryId);
        category.setTenantId(tenantId);
        category.setName(name);
        category.setSlug(name.toLowerCase().replace(" ", "-"));
        category.setDescription(name + " description");
        return categoryRepository.save(category);
    }

    /**
     * Creates a thread in the given tenant.
     * 
     * @param tenantId   The tenant ID
     * @param authorId   The author's member ID
     * @param categoryId The category ID
     * @param title      The thread title
     * @return The saved ThreadEntity with generated UUID
     */
    public ThreadEntity createThread(String tenantId, UUID authorId, UUID categoryId, String title) {
        UUID threadId = UUID.randomUUID();
        ThreadEntity thread = new ThreadEntity();
        thread.setId(threadId);
        thread.setTenantId(tenantId);
        thread.setTitle(title);
        thread.setAuthorId(authorId);
        thread.setCategoryId(categoryId);
        thread.setCreatedAt(Instant.now());
        thread.setStatus(com.openforum.domain.aggregate.ThreadStatus.OPEN);
        thread.setPostCount(0);
        thread.setDeleted(false);
        thread.setLastActivityAt(Instant.now());
        return threadRepository.save(thread);
    }

    /**
     * Creates a post in the given thread.
     * 
     * @param tenantId The tenant ID
     * @param threadId The thread ID
     * @param authorId The author's member ID
     * @param content  The post content
     * @return The saved PostEntity with generated UUID
     */
    public PostEntity createPost(String tenantId, UUID threadId, UUID authorId, String content) {
        UUID postId = UUID.randomUUID();
        PostEntity post = new PostEntity();
        post.setId(postId);
        post.setTenantId(tenantId);
        post.setThreadId(threadId);
        post.setAuthorId(authorId);
        post.setContent(content);
        post.setCreatedAt(Instant.now());
        post.setDeleted(false);
        post.setScore(0);
        return postRepository.save(post);
    }

    /**
     * Convenience method to create the full entity chain.
     * 
     * @param tenantId   The tenant ID
     * @param externalId The member's external ID
     * @return TestData containing all created entities
     */
    public TestData createFullTestData(String tenantId, String externalId) {
        TenantEntity tenant = createTenant(tenantId);
        MemberEntity member = createMember(tenantId, externalId);
        CategoryEntity category = createCategory(tenantId, "Test Category");
        ThreadEntity thread = createThread(tenantId, member.getId(), category.getId(), "Test Thread");
        PostEntity post = createPost(tenantId, thread.getId(), member.getId(), "Test post content");
        return new TestData(tenant, member, category, thread, post);
    }

    /**
     * Record holding all test entities for convenience.
     */
    public record TestData(
            TenantEntity tenant,
            MemberEntity member,
            CategoryEntity category,
            ThreadEntity thread,
            PostEntity post) {
    }
}
