package com.shop.order.domain;

import java.time.Instant;
import java.util.UUID;

public class Order {
  private final UUID id;
  private final String productId;
  private final int quantity;
  private OrderStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  public Order(String productId, int quantity) {
    this(UUID.randomUUID(), productId, quantity, OrderStatus.PENDING, Instant.now(), Instant.now());
  }

  private Order(
      UUID id,
      String productId,
      int quantity,
      OrderStatus status,
      Instant createdAt,
      Instant updatedAt) {
    if (productId == null || productId.isBlank()) {
      throw new IllegalArgumentException("productId must not be blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be greater than zero");
    }
    this.id = id;
    this.productId = productId;
    this.quantity = quantity;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Order restore(
      UUID id,
      String productId,
      int quantity,
      OrderStatus status,
      Instant createdAt,
      Instant updatedAt) {
    return new Order(id, productId, quantity, status, createdAt, updatedAt);
  }

  public void confirm() {
    if (status == OrderStatus.CONFIRMED) {
      return;
    }
    status = OrderStatus.CONFIRMED;
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
