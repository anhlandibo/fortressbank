package com.uit.sharedkernel.outbox;

import com.uit.sharedkernel.outbox.OutboxEventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_status_created", columnList = "status,created_at"),
    @Index(name = "idx_aggregate", columnList = "aggregate_type,aggregate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private java.util.UUID eventId;

    @NotEmpty
    @Column(name = "aggregate_type", length = 100, nullable = false)
    private String aggregateType; // 'transaction', 'account', etc.

    @NotEmpty
    @Column(name = "aggregate_id", length = 100, nullable = false)
    private String aggregateId; // ID của bản ghi

    @NotEmpty
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType; // 'TransactionCreated', 'BalanceUpdated', etc.
    
    @NotEmpty
    @Column(name = "exchange", nullable = false)
    private String exchange;

    @NotEmpty
    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON payload as text

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
