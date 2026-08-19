package com.shop.order.application;

import com.shop.order.domain.Order;
import com.shop.order.domain.OrderNotFoundException;
import com.shop.order.infrastructure.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class GetOrderService {
  private final OrderRepository repository;

  public GetOrderService(OrderRepository repository) {
    this.repository = repository;
  }

  public Order get(String id) {
    return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
  }
}
