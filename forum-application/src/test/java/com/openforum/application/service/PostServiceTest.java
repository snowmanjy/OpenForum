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

        Member author = Member.reconstitute(authorId, "ext-1", "test@example.com", "Test User", false,
                java.time.Instant.now(), com.openforum.domain.valueobject.MemberRole.MEMBER, "test-tenant");
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(author));

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
}
