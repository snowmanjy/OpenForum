package com.openforum.application.service;

import com.openforum.domain.aggregate.Subscription;
import com.openforum.domain.aggregate.Thread;
import com.openforum.domain.repository.SubscriptionRepository;
import com.openforum.domain.repository.ThreadRepository;
import com.openforum.domain.valueobject.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private com.openforum.domain.repository.CategoryRepository categoryRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void should_get_subscriptions_for_user_with_thread_titles() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        String threadTitle = "Test Thread";
        Subscription subscription = Subscription.create("tenant-1", userId, threadId, TargetType.THREAD);

        Thread thread = org.mockito.Mockito.mock(Thread.class);
        when(thread.getTitle()).thenReturn(threadTitle);

        when(subscriptionRepository.findByUserId(eq(userId), anyInt(), anyInt()))
                .thenReturn(List.of(subscription));
        when(threadRepository.findById(threadId)).thenReturn(Optional.of(thread));

        // When
        List<com.openforum.application.dto.SubscriptionDto> result = subscriptionService.getSubscriptionsForUser("tenant-1", userId, 0, 10);

        // Then
                
        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetId()).isEqualTo(threadId);
        assertThat(result.get(0).title()).isEqualTo(threadTitle);
        assertThat(result.get(0).targetType()).isEqualTo(TargetType.THREAD);
    }
}
