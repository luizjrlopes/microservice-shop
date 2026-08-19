package com.shop.order.domain;

public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(String id) {
    super("Order not found: " + id);
  }
}
