package com.shop.order.application.outbox;

import java.time.Instant;
import java.util.UUID;

public record PendingOutboxEvent(
    UUID id,
    String eventType,
    int eventVersion,
    UUID aggregateId,
    UUID correlationId,
    String payload,
    Instant occurredAt,
    int attempts) {}
