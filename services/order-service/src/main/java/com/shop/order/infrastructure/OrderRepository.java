package com.shop.order.infrastructure;

import com.shop.order.domain.Order;

public interface OrderRepository {
    void save(Order order);
}
