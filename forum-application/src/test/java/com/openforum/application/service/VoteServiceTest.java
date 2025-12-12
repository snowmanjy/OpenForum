package com.openforum.application.service;

import com.openforum.domain.repository.VoteRepository;
import com.openforum.domain.repository.VoteRepository.VoteRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    private VoteService voteService;

    private final UUID postId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final String tenantId = "test-tenant";

    @BeforeEach
    void setUp() {
        voteService = new VoteService(voteRepository);
    }

    @Nested
    @DisplayName("vote() method")
    class VoteMethod {

        @Test
        @DisplayName("Scenario A: New upvote creates vote and returns +1")
        void newUpvote_createsVoteAndReturnsPositiveOne() {
            // Given: No existing vote
            when(voteRepository.findByPostIdAndMemberId(postId, memberId)).thenReturn(Optional.empty());

            // When
            int result = voteService.vote(postId, memberId, tenantId, 1);

            // Then
            assertThat(result).isEqualTo(1);
            verify(voteRepository).save(postId, memberId, tenantId, 1);
            verify(voteRepository).updatePostScore(postId, 1);
        }

        @Test
        @DisplayName("Scenario A: New downvote creates vote and returns -1")
        void newDownvote_createsVoteAndReturnsNegativeOne() {
            // Given: No existing vote
            when(voteRepository.findByPostIdAndMemberId(postId, memberId)).thenReturn(Optional.empty());

            // When
            int result = voteService.vote(postId, memberId, tenantId, -1);

            // Then
            assertThat(result).isEqualTo(-1);
            verify(voteRepository).save(postId, memberId, tenantId, -1);
            verify(voteRepository).updatePostScore(postId, -1);
        }

        @Test
        @DisplayName("Scenario B: Changing upvote to downvote returns -2")
        void changeUpvoteToDownvote_returnsNegativeTwo() {
            // Given: Existing upvote
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.of(new VoteRecord(postId, memberId, 1)));

            // When
            int result = voteService.vote(postId, memberId, tenantId, -1);

            // Then
            assertThat(result).isEqualTo(-2);
            verify(voteRepository).update(postId, memberId, -1);
            verify(voteRepository).updatePostScore(postId, -2);
        }

        @Test
        @DisplayName("Scenario B: Changing downvote to upvote returns +2")
        void changeDownvoteToUpvote_returnsPositiveTwo() {
            // Given: Existing downvote
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.of(new VoteRecord(postId, memberId, -1)));

            // When
            int result = voteService.vote(postId, memberId, tenantId, 1);

            // Then
            assertThat(result).isEqualTo(2);
            verify(voteRepository).update(postId, memberId, 1);
            verify(voteRepository).updatePostScore(postId, 2);
        }

        @Test
        @DisplayName("Scenario C: Clicking same upvote removes vote and returns -1")
        void unvoteUpvote_deletesVoteAndReturnsNegativeOne() {
            // Given: Existing upvote
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.of(new VoteRecord(postId, memberId, 1)));

            // When
            int result = voteService.vote(postId, memberId, tenantId, 1);

            // Then
            assertThat(result).isEqualTo(-1);
            verify(voteRepository).delete(postId, memberId);
            verify(voteRepository).updatePostScore(postId, -1);
        }

        @Test
        @DisplayName("Scenario C: Clicking same downvote removes vote and returns +1")
        void unvoteDownvote_deletesVoteAndReturnsPositiveOne() {
            // Given: Existing downvote
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.of(new VoteRecord(postId, memberId, -1)));

            // When
            int result = voteService.vote(postId, memberId, tenantId, -1);

            // Then
            assertThat(result).isEqualTo(1);
            verify(voteRepository).delete(postId, memberId);
            verify(voteRepository).updatePostScore(postId, 1);
        }

        @Test
        @DisplayName("Invalid vote value throws exception")
        void invalidVoteValue_throwsException() {
            assertThatThrownBy(() -> voteService.vote(postId, memberId, tenantId, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Vote value must be 1 or -1");

            assertThatThrownBy(() -> voteService.vote(postId, memberId, tenantId, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Vote value must be 1 or -1");
        }
    }

    @Nested
    @DisplayName("getUserVote() method")
    class GetUserVoteMethod {

        @Test
        @DisplayName("Returns 1 when user has upvoted")
        void returnsOneWhenUpvoted() {
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.of(new VoteRecord(postId, memberId, 1)));

            int result = voteService.getUserVote(postId, memberId);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("Returns -1 when user has downvoted")
        void returnsNegativeOneWhenDownvoted() {
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.of(new VoteRecord(postId, memberId, -1)));

            int result = voteService.getUserVote(postId, memberId);

            assertThat(result).isEqualTo(-1);
        }

        @Test
        @DisplayName("Returns 0 when user has no vote")
        void returnsZeroWhenNoVote() {
            when(voteRepository.findByPostIdAndMemberId(postId, memberId))
                    .thenReturn(Optional.empty());

            int result = voteService.getUserVote(postId, memberId);

            assertThat(result).isEqualTo(0);
        }
    }
}
