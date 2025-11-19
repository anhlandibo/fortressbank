package com.uit.transactionservice.repository;

import com.uit.transactionservice.entity.OutboxEvent;
import com.uit.transactionservice.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, java.util.UUID> {

    List<OutboxEvent> findByStatus(OutboxEventStatus status);

    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxEventStatus status, LocalDateTime before);

    List<OutboxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    List<OutboxEvent> findByEventType(String eventType);
}
