package com.openforum.infra.jpa.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.infra.jpa.entity.OutboxEventEntity;
import com.openforum.infra.jpa.repository.OutboxEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final String TOPIC = "forum-events-v1";

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public OutboxEventPublisher(OutboxEventJpaRepository outboxEventJpaRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            org.springframework.transaction.support.TransactionTemplate transactionTemplate,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.meterRegistry = meterRegistry;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("OutboxEventPublisher initialized!");
    }

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {
        boolean moreData;
        do {
            moreData = Boolean.TRUE.equals(transactionTemplate.execute(status -> processBatch()));
        } while (moreData);
    }

    private boolean processBatch() {
        List<OutboxEventEntity> events = outboxEventJpaRepository.findTop200ByOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return false;
        }

        logger.info("Found {} events to publish", events.size());
        boolean hasErrors = false;

        for (OutboxEventEntity event : events) {
            try {
                String key = extractKey(event.getPayload());
                kafkaTemplate.send(TOPIC, key, event.getPayload()).get(); // Block to ensure send success before delete
                outboxEventJpaRepository.delete(event);
                logger.debug("Published and deleted event: {}", event.getId());
            } catch (Exception e) {
                logger.error("Failed to publish event: {}", event.getId(), e);
                hasErrors = true;
                handleFailure(event, e);
            }
        }
        return !hasErrors;
    }

    private void handleFailure(OutboxEventEntity event, Exception e) {
        int newRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(newRetryCount);

        if (newRetryCount >= 3) {
            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            meterRegistry.counter("outbox.poison_pills").increment();
            logger.error("Event {} marked as FAILED after {} retries. Error: {}", event.getId(), newRetryCount,
                    e.getMessage());
        } else {
            logger.warn("Event {} failed. Retry count: {}", event.getId(), newRetryCount);
        }
        outboxEventJpaRepository.save(event);
    }

    private String extractKey(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("aggregateId")) {
                return node.get("aggregateId").asText();
            } else if (node.has("tenantId")) {
                return node.get("tenantId").asText();
            }
        } catch (Exception e) {
            logger.warn("Failed to extract key from payload", e);
        }
        return null; // Kafka will distribute round-robin if key is null
    }
}
