package com.uit.sharedkernel;

import com.uit.sharedkernel.outbox.OutboxEvent;
import com.uit.sharedkernel.outbox.OutboxEventStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for shared-kernel module.
 * 
 * DESIGN DECISION: This is a LIBRARY module, not a standalone application.
 * 
 * Why we don't use @SpringBootTest here:
 * 1. Libraries provide classes/interfaces - they are NOT runnable applications
 * 2. @SpringBootTest requires full application context with database, messaging, etc.
 * 3. The components here (OutboxScheduler, repositories) are wired by CONSUMING services
 * 4. Testing context loading for a library is meaningless - unit tests are appropriate
 * 
 * Integration tests for these components belong in the services that use them
 * (account-service, transaction-service, etc.)
 */
class SharedKernelApplicationTests {

	@Test
	void outboxEvent_canBeCreated() {
		// Given
		String aggregateType = "Transaction";
		String aggregateId = UUID.randomUUID().toString();
		String eventType = "TransactionCreated";
		String payload = "{\"amount\": 100}";
		String exchange = "transaction.exchange";
		String routingKey = "transaction.created";

		// When
		OutboxEvent event = OutboxEvent.builder()
				.aggregateType(aggregateType)
				.aggregateId(aggregateId)
				.eventType(eventType)
				.exchange(exchange)
				.routingKey(routingKey)
				.payload(payload)
				.status(OutboxEventStatus.PENDING)
				.retryCount(0)
				.createdAt(LocalDateTime.now())
				.build();

		// Then
		assertNotNull(event);
		assertEquals(aggregateType, event.getAggregateType());
		assertEquals(aggregateId, event.getAggregateId());
		assertEquals(eventType, event.getEventType());
		assertEquals(exchange, event.getExchange());
		assertEquals(routingKey, event.getRoutingKey());
		assertEquals(payload, event.getPayload());
		assertEquals(OutboxEventStatus.PENDING, event.getStatus());
		assertEquals(0, event.getRetryCount());
	}

	@Test
	void outboxEventStatus_hasExpectedValues() {
		// Verify enum values exist and are correctly named
		// Values: PENDING, PROCESSING, COMPLETED, FAILED
		assertEquals(4, OutboxEventStatus.values().length);
		assertNotNull(OutboxEventStatus.PENDING);
		assertNotNull(OutboxEventStatus.PROCESSING);
		assertNotNull(OutboxEventStatus.COMPLETED);
		assertNotNull(OutboxEventStatus.FAILED);
	}

	@Test
	void outboxEvent_canUpdateStatus() {
		// Given
		OutboxEvent event = OutboxEvent.builder()
				.aggregateType("Account")
				.aggregateId("acc-123")
				.eventType("AccountCreated")
				.exchange("account.exchange")
				.routingKey("account.created")
				.payload("{}")
				.status(OutboxEventStatus.PENDING)
				.retryCount(0)
				.createdAt(LocalDateTime.now())
				.build();

		// When - simulate processing completion
		event.setStatus(OutboxEventStatus.COMPLETED);
		event.setProcessedAt(LocalDateTime.now());

		// Then
		assertEquals(OutboxEventStatus.COMPLETED, event.getStatus());
		assertNotNull(event.getProcessedAt());
	}

	@Test
	void outboxEvent_canIncrementRetryCount() {
		// Given
		OutboxEvent event = OutboxEvent.builder()
				.aggregateType("Account")
				.aggregateId("acc-123")
				.eventType("AccountCreated")
				.exchange("account.exchange")
				.routingKey("account.created")
				.payload("{}")
				.status(OutboxEventStatus.FAILED)
				.retryCount(0)
				.createdAt(LocalDateTime.now())
				.build();

		// When
		event.setRetryCount(event.getRetryCount() + 1);

		// Then
		assertEquals(1, event.getRetryCount());
	}

	@Test
	void outboxEvent_canRecordErrorMessage() {
		// Given
		OutboxEvent event = OutboxEvent.builder()
				.aggregateType("Transaction")
				.aggregateId("txn-456")
				.eventType("TransactionFailed")
				.exchange("transaction.exchange")
				.routingKey("transaction.failed")
				.payload("{}")
				.status(OutboxEventStatus.PENDING)
				.retryCount(0)
				.createdAt(LocalDateTime.now())
				.build();

		// When - simulate a failure
		String errorMessage = "RabbitMQ connection refused";
		event.setStatus(OutboxEventStatus.FAILED);
		event.setErrorMessage(errorMessage);
		event.setRetryCount(1);

		// Then
		assertEquals(OutboxEventStatus.FAILED, event.getStatus());
		assertEquals(errorMessage, event.getErrorMessage());
		assertEquals(1, event.getRetryCount());
	}
}
