package com.shop.order.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.shop.order.application.outbox.PendingOutboxEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitOutboxEventPublisherTest {

  @Test
  void returnsOnlyAfterBrokerAck() {
    RabbitTemplate template = org.mockito.Mockito.mock(RabbitTemplate.class);
    var publisher =
        new RabbitOutboxEventPublisher(template, 1000, "order.exchange", "order.created");
    var event = pendingEvent();

    doAnswer(
            invocation -> {
              CorrelationData correlationData = invocation.getArgument(3);
              correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
              return null;
            })
        .when(template)
        .send(
            eq("order.exchange"),
            eq("order.created"),
            any(Message.class),
            any(CorrelationData.class));

    publisher.publish(event);

    verify(template)
        .send(
            eq("order.exchange"),
            eq("order.created"),
            any(Message.class),
            any(CorrelationData.class));
  }

  @Test
  void failsWhenBrokerNacksEvent() {
    RabbitTemplate template = org.mockito.Mockito.mock(RabbitTemplate.class);
    var publisher =
        new RabbitOutboxEventPublisher(template, 1000, "order.exchange", "order.created");
    var event = pendingEvent();

    doAnswer(
            invocation -> {
              CorrelationData correlationData = invocation.getArgument(3);
              correlationData
                  .getFuture()
                  .complete(new CorrelationData.Confirm(false, "broker nack"));
              return null;
            })
        .when(template)
        .send(
            eq("order.exchange"),
            eq("order.created"),
            any(Message.class),
            any(CorrelationData.class));

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("broker nack");
  }

  private PendingOutboxEvent pendingEvent() {
    UUID eventId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    return new PendingOutboxEvent(
        eventId,
        "order.created",
        1,
        orderId,
        orderId,
        "{\"eventId\":\"" + eventId + "\"}",
        Instant.now(),
        0);
  }
}
