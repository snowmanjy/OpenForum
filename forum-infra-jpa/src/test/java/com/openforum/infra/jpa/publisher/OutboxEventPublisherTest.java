package com.openforum.infra.jpa.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventJpaRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(repository, kafkaTemplate, objectMapper, transactionTemplate,
                meterRegistry);
    }

    @Test
    void shouldStopProcessingLoopOnPublishError() throws ExecutionException, InterruptedException {
        // Given
        OutboxEventEntity event1 = new OutboxEventEntity();
        event1.setId(UUID.randomUUID());
        event1.setPayload("{}");

        OutboxEventEntity event2 = new OutboxEventEntity();
        event2.setId(UUID.randomUUID());
        event2.setPayload("{}");

        // Mock TransactionTemplate to execute the callback immediately
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        // Mock Repository to return events
        // We return a list of events. If the loop continued, it would call this again.
        // To verify it stops, we can check the number of invocations.
        when(repository.findTop200ByOrderByCreatedAtAsc()).thenReturn(List.of(event1, event2));

        // Mock KafkaTemplate to fail for the first event
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka error"));

        // Use doReturn for the first call (fail) and success for others (if any)
        // Note: In the loop, it iterates over the list.
        // 1st call: fails.
        // 2nd call: succeeds (to verify we continue processing the batch but stop the
        // loop)
        CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture
                .completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(failedFuture)
                .thenReturn(successFuture);

        // When
        publisher.publishEvents();

        // Then
        // 1. Verify repository.findTop200... was called exactly ONCE.
        // If the loop didn't stop, it would have called it again (infinite loop or
        // until we run out of mock responses)
        verify(repository, times(1)).findTop200ByOrderByCreatedAtAsc();

        // 2. Verify we attempted to send both events in the batch
        verify(kafkaTemplate, times(2)).send(any(), any(), any());

        // 3. Verify we ONLY deleted the second event (the successful one)
        verify(repository, times(1)).delete(event2);
        verify(repository, never()).delete(event1);
    }
}
