package com.shop.order.application.ports;

import com.shop.order.application.events.OrderCreatedEventV1;
import com.shop.order.application.outbox.PendingOutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {
  void save(OrderCreatedEventV1 event);

  List<PendingOutboxEvent> findPending(int limit);

  void markPublished(UUID id, Instant publishedAt);

  void recordFailure(UUID id, String error);
}
