package com.shop.order.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.shop.order.application.outbox.PendingOutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
class JpaOutboxEventEntity {
  @Id private UUID id;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "event_version", nullable = false)
  private int eventVersion;

  @Column(name = "correlation_id", nullable = false)
  private UUID correlationId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "last_error")
  private String lastError;

  protected JpaOutboxEventEntity() {}

  JpaOutboxEventEntity(
      UUID id,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      int eventVersion,
      UUID correlationId,
      JsonNode payload,
      Instant occurredAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.eventVersion = eventVersion;
    this.correlationId = correlationId;
    this.payload = payload;
    this.occurredAt = occurredAt;
    this.attempts = 0;
  }

  PendingOutboxEvent toPending() {
    return new PendingOutboxEvent(
        id,
        eventType,
        eventVersion,
        aggregateId,
        correlationId,
        payload.toString(),
        occurredAt,
        attempts);
  }

  void markPublished(Instant instant) {
    publishedAt = instant;
    lastError = null;
  }

  void recordFailure(String error) {
    attempts += 1;
    lastError = error;
  }
}
