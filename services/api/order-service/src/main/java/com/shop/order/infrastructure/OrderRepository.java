package com.shop.order.infrastructure;

import com.shop.order.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    void update(Order order);
}
