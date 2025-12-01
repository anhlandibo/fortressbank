package com.uit.transactionservice.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.sharedkernel.constants.RabbitMQConstants;
import com.uit.transactionservice.entity.OutboxEvent;
import com.uit.transactionservice.entity.OutboxEventStatus;
import com.uit.transactionservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY_COUNT = 3;

    /**
     * Scheduled job to publish pending outbox events to RabbitMQ
     * Runs every 5 seconds
     */
    // @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:5000}")
    @Transactional
    public void publishPendingEvents() {
        log.debug("Starting outbox publisher job...");

        // Find all PENDING events or FAILED events that should be retried
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING);
        List<OutboxEvent> failedEvents = outboxEventRepository
                .findByStatusAndCreatedAtBefore(OutboxEventStatus.FAILED, LocalDateTime.now().minusMinutes(5));

        int totalEvents = pendingEvents.size() + failedEvents.size();
        if (totalEvents == 0) {
            log.debug("No pending events to publish");
            return;
        }

        log.info("Found {} pending events and {} failed events to publish", 
                pendingEvents.size(), failedEvents.size());

        // Process pending events
        pendingEvents.forEach(this::publishEvent);

        // Process failed events that can be retried
        failedEvents.stream()
                .filter(event -> event.getRetryCount() == null || event.getRetryCount() < MAX_RETRY_COUNT)
                .forEach(this::publishEvent);
    }

    /**
     * Publish a single outbox event to RabbitMQ
     */
    private void publishEvent(OutboxEvent event) {
        try {
            log.info("Publishing event: {} for aggregate: {}", event.getEventType(), event.getAggregateId());

            // Update status to PROCESSING
            event.setStatus(OutboxEventStatus.PROCESSING);
            outboxEventRepository.save(event);

            // Parse payload to Map for RabbitMQ
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);

            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.TRANSACTION_EXCHANGE,
                    RabbitMQConstants.TRANSACTION_CREATED,
                    payload
            );

            // Update status to COMPLETED
            event.setStatus(OutboxEventStatus.COMPLETED);
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);

            log.info("Successfully published event: {} for aggregate: {}", 
                    event.getEventType(), event.getAggregateId());

        } catch (Exception e) {
            log.error("Failed to publish event: {} for aggregate: {}. Error: {}", 
                    event.getEventType(), event.getAggregateId(), e.getMessage(), e);

            // Update status to FAILED and increment retry count
            event.setStatus(OutboxEventStatus.FAILED);
            event.setRetryCount(event.getRetryCount() == null ? 1 : event.getRetryCount() + 1);
            event.setErrorMessage(e.getMessage());
            outboxEventRepository.save(event);

            if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                log.error("Event {} has exceeded max retry count. Manual intervention required.", 
                        event.getEventId());
            }
        }
    }
}
