package com.shop.order.application.ports;

import com.shop.order.application.outbox.PendingOutboxEvent;

public interface OutboxEventPublisher {
  void publish(PendingOutboxEvent event);
}
