package com.openforum.application.service;

import com.openforum.domain.aggregate.Member;
import com.openforum.domain.aggregate.Post;
import com.openforum.domain.aggregate.Thread;
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

        Thread thread = org.mockito.Mockito.mock(Thread.class);
        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        Member member = Member.reconstitute(authorId, "ext-1", "test@example.com", "User", false);
        when(memberRepository.findById(authorId)).thenReturn(Optional.of(member));

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Post post = postService.createPost(threadId, authorId, content, null, null);

        // Then
        assertThat(post).isNotNull();
        assertThat(post.getContent()).isEqualTo(content);
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
        assertThatThrownBy(() -> postService.createPost(threadId, authorId, content, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thread not found");
    }
}
