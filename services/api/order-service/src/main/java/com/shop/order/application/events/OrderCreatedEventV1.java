package com.shop.order.application.events;

import com.shop.order.domain.Order;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEventV1(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    UUID correlationId,
    Payload payload) {

  public static OrderCreatedEventV1 from(Order order) {
    return new OrderCreatedEventV1(
        UUID.randomUUID(),
        "order.created",
        1,
        Instant.now(),
        order.getId(),
        new Payload(
            order.getId(),
            order.getProductId(),
            order.getQuantity(),
            order.getStatus().name()));
  }

  public record Payload(UUID orderId, String productId, int quantity, String status) {}
}
