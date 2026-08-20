package com.shop.order.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shop.order.application.outbox.PendingOutboxEvent;
import com.shop.order.application.ports.OutboxEventPublisher;
import com.shop.order.application.ports.OutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishPendingOutboxEventsServiceTest {

  @Mock private OutboxRepository repository;

  @Mock private OutboxEventPublisher publisher;

  private PublishPendingOutboxEventsService service;

  @BeforeEach
  void setUp() {
    service = new PublishPendingOutboxEventsService(repository, publisher, 50);
  }

  @Test
  void marksEventPublishedOnlyAfterPublisherSucceeds() {
    var event = pendingEvent();
    when(repository.findPending(50)).thenReturn(List.of(event));

    service.publishPending();

    verify(publisher).publish(event);
    verify(repository)
        .markPublished(
            org.mockito.ArgumentMatchers.eq(event.id()),
            org.mockito.ArgumentMatchers.any(Instant.class));
    verify(repository, never())
        .recordFailure(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void keepsEventPendingAndRecordsFailureWhenPublisherFails() {
    var event = pendingEvent();
    when(repository.findPending(50)).thenReturn(List.of(event));
    org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
        .when(publisher)
        .publish(event);

    service.publishPending();

    verify(repository, never())
        .markPublished(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verify(repository).recordFailure(event.id(), "broker unavailable");
  }

  private PendingOutboxEvent pendingEvent() {
    UUID id = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    return new PendingOutboxEvent(
        id, "order.created", 1, orderId, orderId, "{\"eventId\":\"" + id + "\"}", Instant.now(), 0);
  }
}
