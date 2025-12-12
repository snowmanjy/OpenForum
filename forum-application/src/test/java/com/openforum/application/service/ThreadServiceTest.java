package com.openforum.application.service;

import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadServiceTest {

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private com.openforum.domain.repository.PostRepository postRepository;

    @Mock
    private com.openforum.domain.repository.CategoryRepository categoryRepository;

    @Mock
    private com.openforum.domain.repository.MemberRepository memberRepository;

    @InjectMocks
    private ThreadService threadService;

    @Test
    void createThread_shouldPersistAndReturnThread() {
        // Given
        String tenantId = "tenant-1";
        UUID authorId = UUID.randomUUID();
        String title = "Test Thread";
        String content = "Test Content";
        UUID categoryId = UUID.randomUUID();

        // Ensure author exists and is valid (Rule 2: Tenant check, Not Banned)
        com.openforum.domain.aggregate.Member author = mock(com.openforum.domain.aggregate.Member.class);
        when(author.getTenantId()).thenReturn(tenantId);
        when(author.getRole()).thenReturn(com.openforum.domain.valueobject.MemberRole.MEMBER);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(mock(com.openforum.domain.aggregate.Category.class)));

        // When
        Thread result = threadService.createThread(tenantId, authorId, title, content, categoryId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getAuthorId()).isEqualTo(authorId);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getId()).isNotNull();

        // Rule 1: No Mock Returns for State - Verify Repository Call
        org.mockito.ArgumentCaptor<Thread> threadCaptor = org.mockito.ArgumentCaptor.forClass(Thread.class);
        verify(threadRepository).save(threadCaptor.capture());
        Thread savedThread = threadCaptor.getValue();

        assertThat(savedThread.getTitle()).isEqualTo(title);
        assertThat(savedThread.getPostCount()).isEqualTo(1); // 1 because OP is created immediately
        assertThat(savedThread.getTenantId()).isEqualTo(tenantId);
        assertThat(savedThread.getAuthorId()).isEqualTo(authorId);

        verify(postRepository).save(any(com.openforum.domain.aggregate.Post.class));
    }

    @Test
    void createThread_shouldFail_whenUserBanned() {
        // Rule 2: The 'Mutant' Check - Banned User
        // Given
        String tenantId = "tenant-1";
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // Mock BANNED user
        com.openforum.domain.aggregate.Member author = mock(com.openforum.domain.aggregate.Member.class);
        when(author.getTenantId()).thenReturn(tenantId);
        when(author.getRole()).thenReturn(com.openforum.domain.valueobject.MemberRole.BANNED);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        // When/Then
        assertThatThrownBy(() -> threadService.createThread(tenantId, authorId, "Title", "Content", categoryId))
                .isInstanceOf(ThreadService.ForbiddenException.class)
                .hasMessageContaining("Banned members cannot create threads");

        verify(threadRepository, never()).save(any());
    }

    @Test
    void createThread_shouldFail_whenTenantSpoofed() {
        // Rule 2: The 'Mutant' Check - Tenant Spoofing
        // Given
        String requestTenantId = "tenant-A";
        String userTenantId = "tenant-B";
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // Mock user from DIFFERENT tenant
        com.openforum.domain.aggregate.Member author = mock(com.openforum.domain.aggregate.Member.class);
        when(author.getTenantId()).thenReturn(userTenantId); // Spoof!
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        // When/Then
        assertThatThrownBy(() -> threadService.createThread(requestTenantId, authorId, "Title", "Content", categoryId))
                .isInstanceOf(ThreadService.ForbiddenException.class)
                .hasMessageContaining("Member does not belong to this tenant");

        verify(threadRepository, never()).save(any());
    }

    @Test
    void createThread_shouldPropagateRepositoryException() {
        // Given
        String tenantId = "t1";
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // Mock valid author
        com.openforum.domain.aggregate.Member author = mock(com.openforum.domain.aggregate.Member.class);
        when(author.getTenantId()).thenReturn(tenantId);
        when(author.getRole()).thenReturn(com.openforum.domain.valueobject.MemberRole.MEMBER);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(mock(com.openforum.domain.aggregate.Category.class)));
        doThrow(new RuntimeException("DB Error")).when(threadRepository).save(any(Thread.class));

        // When/Then
        assertThatThrownBy(() -> threadService.createThread("t1", authorId, "Title", "Content", categoryId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }

    @Test
    void getThread_shouldReturnThread_whenExists() {
        // Given
        UUID id = UUID.randomUUID();
        Thread thread = mock(Thread.class);
        when(threadRepository.findById(id)).thenReturn(Optional.of(thread));

        // When
        Optional<Thread> result = threadService.getThread(id);

        // Then
        assertThat(result).isPresent().contains(thread);
    }

    @Test
    void getThread_shouldReturnEmpty_whenNotExists() {
        // Given
        UUID id = UUID.randomUUID();
        when(threadRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<Thread> result = threadService.getThread(id);

        // Then
        assertThat(result).isEmpty();
    }

    // ================= Update Status Tests =================

    @Test
    void updateStatus_shouldClose_whenModeratorClosesOpenThread() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID modMemberId = UUID.randomUUID();
        String tenantId = "test-tenant";

        Thread openThread = Thread.builder()
                .id(threadId)
                .tenantId(tenantId)
                .authorId(UUID.randomUUID())
                .title("Test Thread")
                .status(com.openforum.domain.aggregate.ThreadStatus.OPEN)
                .build();

        when(threadRepository.findByIdAndTenantId(threadId, tenantId)).thenReturn(Optional.of(openThread));

        // When
        Thread result = threadService.updateStatus(threadId, tenantId, modMemberId,
                com.openforum.domain.valueobject.MemberRole.MODERATOR,
                com.openforum.domain.aggregate.ThreadStatus.CLOSED, "Off-topic");

        // Then
        assertThat(result.getStatus()).isEqualTo(com.openforum.domain.aggregate.ThreadStatus.CLOSED);
        verify(threadRepository).save(any(Thread.class));
    }

    @Test
    void updateStatus_shouldOpen_whenAdminOpensClosedThread() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID adminMemberId = UUID.randomUUID();
        String tenantId = "test-tenant";

        Thread closedThread = Thread.builder()
                .id(threadId)
                .tenantId(tenantId)
                .authorId(UUID.randomUUID())
                .title("Test Thread")
                .status(com.openforum.domain.aggregate.ThreadStatus.CLOSED)
                .build();

        when(threadRepository.findByIdAndTenantId(threadId, tenantId)).thenReturn(Optional.of(closedThread));

        // When
        Thread result = threadService.updateStatus(threadId, tenantId, adminMemberId,
                com.openforum.domain.valueobject.MemberRole.ADMIN,
                com.openforum.domain.aggregate.ThreadStatus.OPEN, "Issue resolved");

        // Then
        assertThat(result.getStatus()).isEqualTo(com.openforum.domain.aggregate.ThreadStatus.OPEN);
        verify(threadRepository).save(any(Thread.class));
    }

    @Test
    void updateStatus_shouldFail_whenRegularMemberTriesToClose() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String tenantId = "test-tenant";

        // When & Then (no need to mock repo since permission check happens first)
        assertThatThrownBy(() -> threadService.updateStatus(threadId, tenantId, memberId,
                com.openforum.domain.valueobject.MemberRole.MEMBER,
                com.openforum.domain.aggregate.ThreadStatus.CLOSED, null))
                .isInstanceOf(ThreadService.ForbiddenException.class)
                .hasMessageContaining("Only moderators and admins");
    }

    @Test
    void updateStatus_shouldFail_whenThreadNotFound() {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID modMemberId = UUID.randomUUID();
        String tenantId = "test-tenant";

        when(threadRepository.findByIdAndTenantId(threadId, tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> threadService.updateStatus(threadId, tenantId, modMemberId,
                com.openforum.domain.valueobject.MemberRole.MODERATOR,
                com.openforum.domain.aggregate.ThreadStatus.CLOSED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thread not found");
    }
}
