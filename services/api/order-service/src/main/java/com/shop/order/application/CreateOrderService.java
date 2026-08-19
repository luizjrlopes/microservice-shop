package com.shop.order.application;

import com.shop.order.application.events.OrderCreatedEventV1;
import com.shop.order.application.ports.OrderRepository;
import com.shop.order.application.ports.OutboxRepository;
import com.shop.order.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOrderService {
  private final OrderRepository orderRepository;
  private final OutboxRepository outboxRepository;

  public CreateOrderService(OrderRepository orderRepository, OutboxRepository outboxRepository) {
    this.orderRepository = orderRepository;
    this.outboxRepository = outboxRepository;
  }

  @Transactional
  public Order create(String productId, int quantity) {
    Order order = new Order(productId, quantity);
    orderRepository.save(order);
    outboxRepository.save(OrderCreatedEventV1.from(order));
    return order;
  }
}
