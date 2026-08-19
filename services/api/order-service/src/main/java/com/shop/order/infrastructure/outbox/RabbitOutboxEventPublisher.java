package com.shop.order.infrastructure.outbox;

import com.shop.order.application.outbox.PendingOutboxEvent;
import com.shop.order.application.ports.OutboxEventPublisher;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitOutboxEventPublisher implements OutboxEventPublisher {
  private static final String TRANSITIONAL_EXCHANGE = "order.exchange";
  private static final String TRANSITIONAL_ROUTING_KEY = "order.created";

  private final RabbitTemplate rabbitTemplate;
  private final long confirmTimeoutMs;

  public RabbitOutboxEventPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${outbox.publisher.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
    this.rabbitTemplate = rabbitTemplate;
    this.confirmTimeoutMs = confirmTimeoutMs;
  }

  @Override
  public void publish(PendingOutboxEvent event) {
    var message =
        MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON)
            .setContentEncoding(StandardCharsets.UTF_8.name())
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .setMessageId(event.id().toString())
            .setCorrelationId(event.correlationId().toString())
            .setHeader("eventType", event.eventType())
            .setHeader("eventVersion", event.eventVersion())
            .build();

    var correlationData = new CorrelationData(event.id().toString());
    rabbitTemplate.send(TRANSITIONAL_EXCHANGE, TRANSITIONAL_ROUTING_KEY, message, correlationData);

    try {
      var confirm = correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
      if (!confirm.isAck()) {
        throw new IllegalStateException("RabbitMQ rejected event: " + confirm.getReason());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for RabbitMQ confirm", exception);
    } catch (ExecutionException | TimeoutException exception) {
      throw new IllegalStateException("RabbitMQ publisher confirm failed", exception);
    }
  }
}
