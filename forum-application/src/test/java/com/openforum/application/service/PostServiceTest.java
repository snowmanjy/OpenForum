package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.aggregate.ThreadStatus;
import com.openforum.domain.repository.MemberRepository;
import com.openforum.domain.repository.PostRepository;
import com.openforum.domain.repository.ThreadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import com.openforum.domain.valueobject.MemberRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

        @Mock
        private PostRepository postRepository;

        @Mock
        private ThreadRepository threadRepository;

        @Mock
        private MemberRepository memberRepository;

        @InjectMocks
        private PostService postService;

        @Test
        void createPost_shouldSucceed_whenThreadExists() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String content = "Test Content";

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId("test-tenant")
                                .status(ThreadStatus.OPEN)
                                .postCount(5) // Initial count
                                .build();

                when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

                when(memberRepository.findById(authorId)).thenReturn(Optional.of(Member.reconstitute(
                                authorId,
                                "user1",
                                "user@example.com",
                                "Test User",
                                false,
                                Instant.now(),
                                Instant.now(), // createdAt
                                MemberRole.MEMBER,
                                "test-tenant",
                                null,
                                0,
                                null,
                                null,
                                null)));

                when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                        Post savedPost = invocation.getArgument(0);
                        // Simulate repository returning the saved entity
                        return savedPost;
                });

                // When
                Post post = postService.createPost(threadId, authorId, content, null, null, List.of());

                // Then
                assertThat(post).isNotNull();
                assertThat(post.getContent()).isEqualTo(content);
                assertThat(post.getPostNumber()).isEqualTo(6); // Should be 5 + 1

                assertThat(thread.getPostCount()).isEqualTo(6);
                verify(threadRepository).save(thread); // Verify thread is saved (for lock/count)
                verify(postRepository).save(any(Post.class));
        }

        @Test
        void createPost_shouldFail_whenThreadDoesNotExist() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String content = "Test Content";

                when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> postService.createPost(threadId, authorId, content, null, null, List.of()))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Thread not found");
        }

        // ================= Update Post Tests =================

        @Test
        void updatePost_shouldSucceed_whenAuthorEditsOwnPost() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String tenantId = "test-tenant";
                String newContent = "Updated content";

                Post existingPost = Post.builder()
                                .id(postId)
                                .threadId(UUID.randomUUID())
                                .tenantId(tenantId)
                                .authorId(authorId)
                                .content("Original content")
                                .build();

                when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.of(existingPost));
                when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

                // When
                Post result = postService.updatePost(postId, tenantId, authorId, newContent);

                // Then
                assertThat(result.getContent()).isEqualTo(newContent);
                verify(postRepository).save(any(Post.class));
        }

        @Test
        void updatePost_shouldFail_whenNonAuthorTriesToEdit() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                UUID differentMemberId = UUID.randomUUID();
                String tenantId = "test-tenant";

                Post existingPost = Post.builder()
                                .id(postId)
                                .threadId(UUID.randomUUID())
                                .tenantId(tenantId)
                                .authorId(authorId)
                                .content("Original content")
                                .build();

                when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.of(existingPost));

                // When & Then
                assertThatThrownBy(() -> postService.updatePost(postId, tenantId, differentMemberId, "New content"))
                                .isInstanceOf(PostService.ForbiddenException.class)
                                .hasMessageContaining("You can only edit your own posts");
        }

        @Test
        void updatePost_shouldFail_whenPostNotFound() {
                // Given
                UUID postId = UUID.randomUUID();
                String tenantId = "test-tenant";
                UUID memberId = UUID.randomUUID();

                when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> postService.updatePost(postId, tenantId, memberId, "New content"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Post not found");
        }

        // ================= Delete Post Tests =================

        @Test
        void deletePost_shouldSucceed_whenAuthorDeletesOwnPost() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String tenantId = "test-tenant";

                Post existingPost = Post.builder()
                                .id(postId)
                                .threadId(UUID.randomUUID())
                                .tenantId(tenantId)
                                .authorId(authorId)
                                .content("Original content")
                                .build();

                when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.of(existingPost));

                // When
                postService.deletePost(postId, tenantId, authorId, "User requested");

                // Then
                verify(postRepository).save(any(Post.class));
                assertThat(existingPost.isDeleted()).isTrue();
        }

        @Test
        void deletePost_shouldFail_whenNonAuthorTriesToDelete() {
                // Given
                UUID postId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                UUID differentMemberId = UUID.randomUUID();
                String tenantId = "test-tenant";

                Post existingPost = Post.builder()
                                .id(postId)
                                .threadId(UUID.randomUUID())
                                .tenantId(tenantId)
                                .authorId(authorId)
                                .content("Original content")
                                .build();

                when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.of(existingPost));

                // When & Then
                assertThatThrownBy(() -> postService.deletePost(postId, tenantId, differentMemberId, null))
                                .isInstanceOf(PostService.ForbiddenException.class)
                                .hasMessageContaining("You can only delete your own posts");
        }

        @Test
        void deletePost_shouldFail_whenPostNotFound() {
                // Given
                UUID postId = UUID.randomUUID();
                String tenantId = "test-tenant";
                UUID memberId = UUID.randomUUID();

                when(postRepository.findByIdAndTenantId(postId, tenantId)).thenReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> postService.deletePost(postId, tenantId, memberId, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Post not found");
        }

        @Test
        void createReply_ShouldUpdateThreadLastActivityAt() {
                // Given
                UUID threadId = UUID.randomUUID();
                UUID authorId = UUID.randomUUID();
                String tenantId = "test-tenant";
                String content = "Reply Content";

                Thread thread = Thread.builder()
                                .id(threadId)
                                .tenantId(tenantId)
                                .status(ThreadStatus.OPEN)
                                .postCount(5)
                                .createdAt(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))
                                .lastActivityAt(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))
                                .build();

                when(threadRepository.findByIdWithLock(threadId, tenantId)).thenReturn(Optional.of(thread));

                Member author = Member.reconstitute(authorId, "ext-1", "test@example.com", "Test User", false,
                                Instant.now(), Instant.now(), MemberRole.MEMBER, tenantId,
                                null, 0, null, null, null);
                when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

                when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

                // When
                postService.createReply(threadId, authorId, tenantId, content, null, null, List.of());

                // Then
                org.mockito.ArgumentCaptor<Thread> threadCaptor = org.mockito.ArgumentCaptor.forClass(Thread.class);
                verify(threadRepository).save(threadCaptor.capture());

                Thread savedThread = threadCaptor.getValue();
                assertThat(savedThread.getLastActivityAt()).isCloseTo(java.time.Instant.now(),
                                org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
                assertThat(savedThread.getPostCount()).isEqualTo(6);
        }
}
