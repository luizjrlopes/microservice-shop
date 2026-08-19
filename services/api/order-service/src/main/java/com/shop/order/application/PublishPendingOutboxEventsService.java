package com.shop.order.application;

import com.shop.order.application.ports.OutboxEventPublisher;
import com.shop.order.application.ports.OutboxRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PublishPendingOutboxEventsService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PublishPendingOutboxEventsService.class);

  private final OutboxRepository repository;
  private final OutboxEventPublisher publisher;
  private final int batchSize;

  public PublishPendingOutboxEventsService(
      OutboxRepository repository,
      OutboxEventPublisher publisher,
      @Value("${outbox.publisher.batch-size:50}") int batchSize) {
    this.repository = repository;
    this.publisher = publisher;
    this.batchSize = batchSize;
  }

  @Scheduled(
      fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}",
      initialDelayString = "${outbox.publisher.initial-delay-ms:1000}")
  public void publishPending() {
    for (var event : repository.findPending(batchSize)) {
      try {
        publisher.publish(event);
        repository.markPublished(event.id(), Instant.now());
      } catch (RuntimeException exception) {
        repository.recordFailure(event.id(), exception.getMessage());
        LOGGER.warn(
            "Failed to publish outbox event id={} type={} version={} attempts={}",
            event.id(),
            event.eventType(),
            event.eventVersion(),
            event.attempts() + 1,
            exception);
      }
    }
  }
}
