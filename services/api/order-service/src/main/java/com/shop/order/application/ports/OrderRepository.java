package com.shop.order.application.ports;

import com.shop.order.domain.Order;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
  void save(Order order);

  Optional<Order> findById(UUID id);

  void update(Order order);
}
