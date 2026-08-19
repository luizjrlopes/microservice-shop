package com.shop.order.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.application.events.OrderCreatedEventV1;
import com.shop.order.application.outbox.PendingOutboxEvent;
import com.shop.order.application.ports.OutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresOutboxRepositoryAdapter implements OutboxRepository {
  private final SpringDataOutboxJpaRepository repository;
  private final ObjectMapper objectMapper;

  public PostgresOutboxRepositoryAdapter(
      SpringDataOutboxJpaRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void save(OrderCreatedEventV1 event) {
    var payload = objectMapper.valueToTree(event);
    repository.save(
        new JpaOutboxEventEntity(
            event.eventId(),
            "Order",
            event.payload().orderId(),
            event.eventType(),
            event.eventVersion(),
            event.correlationId(),
            payload,
            event.occurredAt()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingOutboxEvent> findPending(int limit) {
    return repository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, limit)).stream()
        .map(JpaOutboxEventEntity::toPending)
        .toList();
  }

  @Override
  @Transactional
  public void markPublished(UUID id, Instant publishedAt) {
    repository.findById(id).ifPresent(entity -> entity.markPublished(publishedAt));
  }

  @Override
  @Transactional
  public void recordFailure(UUID id, String error) {
    repository.findById(id).ifPresent(entity -> entity.recordFailure(error));
  }
}
