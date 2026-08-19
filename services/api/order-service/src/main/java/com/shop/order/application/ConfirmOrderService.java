package com.shop.order.application;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import com.shop.order.domain.OrderNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConfirmOrderService {
  private final OrderRepository repository;

  public ConfirmOrderService(OrderRepository repository) {
    this.repository = repository;
  }

  public Order confirm(UUID id) {
    Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    order.confirm();
    repository.update(order);
    return order;
  }
}
