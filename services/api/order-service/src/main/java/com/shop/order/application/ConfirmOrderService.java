package com.shop.order.application;

import com.shop.order.domain.Order;
import com.shop.order.domain.OrderNotFoundException;
import com.shop.order.infrastructure.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class ConfirmOrderService {
  private final OrderRepository repository;

  public ConfirmOrderService(OrderRepository repository) {
    this.repository = repository;
  }

  public Order confirm(String id) {
    Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    if ("CONFIRMED".equals(order.getStatus())) {
      return order;
    }
    order.setStatus("CONFIRMED");
    repository.update(order);
    return order;
  }
}
