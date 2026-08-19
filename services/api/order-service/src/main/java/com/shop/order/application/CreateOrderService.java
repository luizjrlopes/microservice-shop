package com.shop.order.application;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import org.springframework.stereotype.Service;

@Service
public class CreateOrderService {
  private final OrderRepository repository;

  public CreateOrderService(OrderRepository repository) {
    this.repository = repository;
  }

  public Order create(String productId, int quantity) {
    Order order = new Order(productId, quantity);
    repository.save(order);
    return order;
  }
}
