package com.openforum.infra.jpa.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { OutboxEventPublisher.class, ObjectMapper.class })
@ActiveProfiles("test")
public class OutboxPoisonPillTest {

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        // Mock TransactionTemplate to execute the callback immediately
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Mock MeterRegistry counter
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter("outbox.poison_pills")).thenReturn(counter);
    }

    @Test
    void testPoisonPillHandling() {
        // Arrange
        UUID badEventId = UUID.randomUUID();
        OutboxEventEntity badEvent = new OutboxEventEntity(badEventId, UUID.randomUUID(), "BadEvent",
                "{\"data\":\"bad\"}", Instant.now());

        UUID goodEventId = UUID.randomUUID();
        OutboxEventEntity goodEvent = new OutboxEventEntity(goodEventId, UUID.randomUUID(), "GoodEvent",
                "{\"data\":\"good\"}", Instant.now());

        // Mock Repository to return events
        // 1st call: returns both (Good event succeeds and is deleted)
        // 2nd call: returns only bad event (Good event is gone)
        // 3rd call: returns only bad event
        when(outboxEventJpaRepository.findTop200ByOrderByCreatedAtAsc())
                .thenReturn(java.util.List.of(badEvent, goodEvent))
                .thenReturn(java.util.List.of(badEvent))
                .thenReturn(java.util.List.of(badEvent));

        // Mock Kafka to fail for bad event and succeed for good event
        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka Error"));

        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> successFuture = new CompletableFuture<>();
        successFuture.complete(null);

        when(kafkaTemplate.send(any(), any(), eq(badEvent.getPayload()))).thenReturn(failedFuture);
        when(kafkaTemplate.send(any(), any(), eq(goodEvent.getPayload()))).thenReturn(successFuture);

        // Act - Run 3 times to trigger poison pill logic
        // 1st run
        outboxEventPublisher.publishEvents();
        // 2nd run
        outboxEventPublisher.publishEvents();
        // 3rd run (Should mark as FAILED)
        outboxEventPublisher.publishEvents();

        // Assert
        // Bad event should be saved 3 times (updating retry count/status)
        verify(outboxEventJpaRepository, times(3)).save(badEvent);

        // Good event should be deleted once (it succeeds on the first run)
        verify(outboxEventJpaRepository, times(1)).delete(goodEvent);

        // Verify bad event state
        assertThat(badEvent.getRetryCount()).isEqualTo(3);
        assertThat(badEvent.getStatus()).isEqualTo("FAILED");
        assertThat(badEvent.getErrorMessage()).contains("Kafka Error");

        // Verify metric incremented
        verify(meterRegistry.counter("outbox.poison_pills"), times(1)).increment();
    }
}
