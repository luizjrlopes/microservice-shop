package com.shop.order.infrastructure;

import com.shop.order.domain.Order;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
  private final RabbitTemplate rabbitTemplate;

  public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(Order order) {
    String eventId = UUID.randomUUID().toString();
    String occurredAt = Instant.now().toString();
    var event =
        new OrderCreatedEvent(
            eventId,
            occurredAt,
            order.getId(),
            order.getProductId(),
            order.getQuantity(),
            order.getStatus());

    rabbitTemplate.convertAndSend(
        MessagingConfig.ORDER_EXCHANGE,
        MessagingConfig.ORDER_CREATED_ROUTING_KEY,
        event,
        message -> {
          message.getMessageProperties().setMessageId(eventId);
          message.getMessageProperties().setCorrelationId(order.getId());
          message.getMessageProperties().setHeader("eventType", "order.created");
          message.getMessageProperties().setHeader("eventId", eventId);
          message.getMessageProperties().setHeader("occurredAt", occurredAt);
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          return message;
        });
  }

  public record OrderCreatedEvent(
      String eventId,
      String occurredAt,
      String id,
      String productId,
      int quantity,
      String status) {}
}
