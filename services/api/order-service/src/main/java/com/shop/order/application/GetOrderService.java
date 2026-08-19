package com.shop.order.application;

import com.shop.order.application.ports.OrderRepository;
import com.shop.order.domain.Order;
import com.shop.order.domain.OrderNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetOrderService {
  private final OrderRepository repository;

  public GetOrderService(OrderRepository repository) {
    this.repository = repository;
  }

  public Order get(UUID id) {
    return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
  }
}
