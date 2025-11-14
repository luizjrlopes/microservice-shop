package com.shop.order.infrastructure;

import com.shop.order.domain.Order;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
  private final RabbitTemplate rabbitTemplate;

  public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(Order order) {
    var event =
        new OrderCreatedEvent(
            order.getId(), order.getProductId(), order.getQuantity(), order.getStatus());
    rabbitTemplate.convertAndSend("order.exchange", "order.created", event);
  }

  public record OrderCreatedEvent(String id, String productId, int quantity, String status) {}
}
